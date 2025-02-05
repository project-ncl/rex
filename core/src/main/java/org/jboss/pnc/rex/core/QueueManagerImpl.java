/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.counter.Counter;
import org.jboss.pnc.rex.core.counter.MaxConcurrent;
import org.jboss.pnc.rex.core.counter.Running;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@ApplicationScoped
public class QueueManagerImpl implements QueueManager {

    private final Counter max;
    private final Counter running;
    private final TaskRegistry container;
    private final TaskController controller;
    private final FaultToleranceDecorator ft;

    public QueueManagerImpl(@MaxConcurrent Counter max,
                            @Running Counter running,
                            TaskRegistry container,
                            TaskController controller,
                            FaultToleranceDecorator ft) {
        this.max = max;
        this.running = running;
        this.container = container;
        this.controller = controller;
        this.ft = ft;
    }

    @Override
    @Transactional
    public void poke() {
        log.info("QUEUE: Poking Task queues");
        Map<String, Long> maxEntries = max.entries();
        Map<String, Long> runningEntries = running.entries();

        Set<String> consideredQueues = new HashSet<>();
        for (var queue : maxEntries.keySet()) {
            Long maxValue = maxEntries.get(queue);
            Long runningValue = runningEntries.get(queue);

            if (runningValue >= maxValue) {
                log.debug("QUEUE '{}': Maximum number of parallel tasks reached.({} out of {})",
                        queue == null ? "DEFAULT" : queue,
                        runningValue,
                        maxValue);
            } else {
                consideredQueues.add(queue);
            }
        }

        for (var queue : consideredQueues) {
            Long maxValue = maxEntries.get(queue);
            VersionedValue<Long> runningMetadata = running.getMetadataValue(queue);
            Long runningValue = runningMetadata.getValue();

            long freeSpace = maxValue - runningValue;
            List<Task> randomEnqueuedTasks = container.getEnqueuedTasksByQueueName(queue, freeSpace);
            if (randomEnqueuedTasks.isEmpty()) {
                continue;
            }

            log.info("QUEUE '{}': Free space of {} found. Scheduling {} task(s) of {}",
                    queue == null ? "DEFAULT" : queue,
                    freeSpace,
                    randomEnqueuedTasks.size(),
                    randomEnqueuedTasks.stream().map(Task::getName).collect(Collectors.toList())
            );

            randomEnqueuedTasks.forEach(task -> controller.dequeue(task.getName()));

            log.info("QUEUE '{}': Increasing running counter. ({} to {}) [ISPN-VERSION:{}]",
                    queue == null ? "DEFAULT" : queue,
                    runningValue,
                    (runningValue + randomEnqueuedTasks.size()),
                    runningMetadata.getVersion());
            if (!running.replaceValue(queue, runningMetadata, runningValue + randomEnqueuedTasks.size())) {
                RuntimeException e = new ConcurrentModificationException("Running counter was modified concurrently.");
                log.error("QUEUE '{}': Concurrent modification detected.", queue == null ? "DEFAULT" : queue, e);
                throw e;
            }
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void decreaseRunningCounter(@Nullable String name) {
        VersionedValue<Long> runningMetadata = running.getMetadataValue(name);
        long runningValue = runningMetadata.getValue() - 1;
        log.info("QUEUE '{}': Decreasing running counter by one. ({} to {}) [ISPN-VERSION:{}]",
                name == null ? "DEFAULT" : name,
                runningMetadata.getValue(),
                runningValue,
                runningMetadata.getVersion());
        if (!running.replaceValue(name, runningMetadata, runningValue)) {
            RuntimeException e = new ConcurrentModificationException("Running counter was modified concurrently.");
            log.error("QUEUE: Concurrent modification detected.", e);
            throw e;
        }
    }

    @Override
    public void setMaximumConcurrency(@Nullable String name, Long amount) {
        // we have to separate these 2 steps into distinct transactions because Counter#entries() doesnt return updated
        // max Counter values in #poke()
        ft.withTolerance(() -> QuarkusTransaction.requiringNew().run(() -> {
            VersionedValue<Long> maxMetadata = max.getMetadataValue(name);
            if (maxMetadata == null) {
                initializeNamedQueue(name, amount);
            } else {
                max.replaceValue(name, maxMetadata, amount);
            }

        }));

        ft.withTolerance(() -> QuarkusTransaction.requiringNew().run(this::poke));
    }

    @Override
    public Long getMaximumConcurrency(@Nullable String name) {
        VersionedValue<Long> meta = max.getMetadataValue(name);
        return meta == null ? null : meta.getValue();
    }

    @Override
    public Long getRunningCounter(@Nullable String name) {
        VersionedValue<Long> meta = running.getMetadataValue(name);
        return meta == null ? null : meta.getValue();
    }

    @Override
    @Transactional(MANDATORY)
    public void synchronizeRunningCounter() {
        Map<String, List<Task>> tasksByQueue = container.getTasks(false, false, true, false, null)
                .stream()
                .collect(groupingBy(Task::getQueue));


        Set<String> existingQueues = running.entries().keySet();
        for (String queue : existingQueues) {
            var runningValue = running.getMetadataValue(queue);
            var tasksInQueue = tasksByQueue.get(queue);

            long actualValue;
            if (tasksInQueue == null) {
                actualValue = 0L;
            } else {
                actualValue = tasksInQueue.size();
            }

            if (!runningValue.getValue().equals(actualValue)) {
                log.info("Synchronizing running counter. Mismatch between active tasks and counter found. Previous value '{}' -> new value '{}'", runningValue.getValue(), actualValue);
                running.replaceValue(queue, runningValue, actualValue);
            }

        }
    }

    private void initializeNamedQueue(String name, Long amount) {
        max.initialize(name, amount);
        running.initialize(name, 0L);
    }
}
