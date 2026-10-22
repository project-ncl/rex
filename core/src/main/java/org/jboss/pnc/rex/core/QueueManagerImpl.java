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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

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
            if (running.requiresReservation()) {
                pokeWithReservation(queue, maxValue);
            } else {
                pokeLegacy(queue, maxValue);
            }
        }
    }

    /**
     * Historical order for optimistic compare-and-swap counters: dequeue first, then increment.
     * The shared-key version check serializes admission across nodes (a stale increment fails and
     * the whole transaction is retried), so no over-admission can happen.
     */
    private void pokeLegacy(String queue, Long maxValue) {
        long runningValue = running.get(queue);
        if (runningValue >= maxValue) {
            log.debug("QUEUE '{}': Maximum number of parallel tasks reached.({} out of {})",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue,
                    runningValue,
                    maxValue);
            return;
        }

        long freeSpace = maxValue - runningValue;
        List<Task> randomEnqueuedTasks = container.getEnqueuedTasksByQueueName(queue, freeSpace);
        if (randomEnqueuedTasks.isEmpty()) {
            return;
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

    /**
     * Order for atomic, non-transactional counters (e.g. StrongCounter): reserve capacity first,
     * release any overshoot, then dequeue only the amount we are allowed to start. This prevents
     * two nodes from both observing the same free space and over-admitting.
     */
    private void pokeWithReservation(String queue, Long maxValue) {
        long runningValue = running.get(queue);
        if (runningValue >= maxValue) {
            log.debug("QUEUE '{}': Maximum number of parallel tasks reached.({} out of {})",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue,
                    runningValue,
                    maxValue);
            return;
        }

        long freeSpace = maxValue - runningValue;
        List<Task> candidates = container.getEnqueuedTasksByQueueName(queue, freeSpace);
        if (candidates.isEmpty()) {
            return;
        }

        int want = candidates.size();
        long afterReserve = running.reserve(queue, want);
        int allowed = want;
        if (afterReserve > maxValue) {
            long giveBack = Math.min(afterReserve - maxValue, want);
            running.release(queue, giveBack);
            allowed = (int) (want - giveBack);
        }
        if (allowed <= 0) {
            return;
        }

        List<Task> toSchedule = candidates.subList(0, allowed);
        log.info("QUEUE '{}': Reserved {} slot(s). Scheduling {} task(s) of {}",
                queue == null ? DEFAULT_QUEUE_NAMING : queue,
                allowed,
                toSchedule.size(),
                toSchedule.stream().map(Task::getName).collect(Collectors.toList())
        );

        try {
            toSchedule.forEach(task -> controller.dequeue(task.getName()));
        } catch (RuntimeException e) {
            // The transaction rolls back every dequeue done in this invocation; release the whole
            // reservation so the (non-transactional) counter does not drift.
            running.release(queue, allowed);
            throw e;
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
        // null-safe grouping: DEFAULT queue tasks have a null queue, which Collectors.groupingBy rejects
        Map<String, List<Task>> tasksByQueue = new HashMap<>();
        for (Task task : container.getTasks(false, false, true, false, false, null)) {
            tasksByQueue.computeIfAbsent(task.getQueue(), q -> new ArrayList<>()).add(task);
        }

        // reconcile every known queue. Max counters survive a restart (persistent cache), while the
        // running counters may not (e.g. volatile StrongCounters), so union both sources.
        Set<String> queues = new HashSet<>(max.entries().keySet());
        queues.addAll(running.queues());

        for (String queue : queues) {
            List<Task> tasksInQueue = tasksByQueue.get(queue);
            long actualValue = tasksInQueue == null ? 0L : tasksInQueue.size();
            running.reconcile(queue, actualValue);
        }
    }

    private void initializeNamedQueue(String name, Long amount) {
        max.initialize(name, amount);
        running.initialize(name, 0L);
    }
}
