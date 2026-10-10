/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.pnc.rex.core.counter.RunningTracker;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@ApplicationScoped
public class QueueManagerImpl implements QueueManager {

    private static final String DEFAULT_QUEUE_NAMING = "DEFAULT";
    private final Counter max;
    private final RunningTracker running;
    private final TaskRegistry container;
    private final TaskController controller;
    private final FaultToleranceDecorator ft;

    public QueueManagerImpl(@MaxConcurrent Counter max,
                            RunningTracker running,
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

        for (var queue : maxEntries.keySet()) {
            Long maxValue = maxEntries.get(queue);
            long runningValue = running.get(queue);

            if (runningValue >= maxValue) {
                log.debug("QUEUE '{}': Maximum number of parallel tasks reached.({} out of {})",
                        queue == null ? DEFAULT_QUEUE_NAMING : queue,
                        runningValue,
                        maxValue);
                continue;
            }

            long freeSpace = maxValue - runningValue;
            List<Task> randomEnqueuedTasks = container.getEnqueuedTasksByQueueName(queue, freeSpace);
            if (randomEnqueuedTasks.isEmpty()) {
                continue;
            }

            log.info("QUEUE '{}': Free space of {} found. Scheduling {} task(s) of {}",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue,
                    freeSpace,
                    randomEnqueuedTasks.size(),
                    randomEnqueuedTasks.stream().map(Task::getName).collect(Collectors.toList())
            );

            randomEnqueuedTasks.forEach(task -> controller.dequeue(task.getName()));

            long newRunning = running.reserve(queue, randomEnqueuedTasks.size());
            log.info("QUEUE '{}': Increased running counter to {}.",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue,
                    newRunning);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void decreaseRunningCounter(@Nullable String name) {
        running.decrement(name);
        log.info("QUEUE '{}': Decreased running counter by one.", name == null ? DEFAULT_QUEUE_NAMING : name);
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
        return running.peek(name);
    }

    @Override
    @Transactional(MANDATORY)
    public void synchronizeRunningCounter() {
        Map<String, List<Task>> tasksByQueue = container.getTasks(false, false, true, false, false, null)
                .stream()
                .collect(groupingBy(Task::getQueue));

        for (String queue : running.queues()) {
            var tasksInQueue = tasksByQueue.get(queue);
            long actualValue = tasksInQueue == null ? 0L : tasksInQueue.size();
            running.reconcile(queue, actualValue);
        }
    }

    private void initializeNamedQueue(String name, Long amount) {
        max.initialize(name, amount);
        running.initialize(name, 0L);
    }
}
