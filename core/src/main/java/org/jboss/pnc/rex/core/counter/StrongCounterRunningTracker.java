/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.SyncStrongCounter;
import org.jboss.pnc.rex.core.common.Constants;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Infinispan clustered {@link org.infinispan.counter.api.StrongCounter} implementation of
 * {@link RunningTracker}.
 * <p>
 * Each queue maps to one unbounded strong counter ({@code RUNNING} for the DEFAULT queue,
 * {@code RUNNING-<queue>} for named queues). Increments/decrements are atomic cluster-wide, which
 * removes the single-key compare-and-swap contention / pessimistic-lock timeouts of the cache-entry
 * based implementation.
 * <p>
 * The counters are <b>not</b> transactional, so admission uses reserve-then-compensate
 * ({@link #requiresReservation()} is {@code true}) and drift is healed by
 * {@code QueueManager#synchronizeRunningCounter()} (run at startup and on demand).
 * <p>
 * Storage is {@link Storage#VOLATILE}: counters are rebuilt from live task state by reconciliation
 * after a full-cluster restart.
 * <p>
 * Instantiated by {@link RunningTrackerProducer} (not a CDI bean itself).
 */
@Slf4j
public class StrongCounterRunningTracker implements RunningTracker {

    private static final String DEFAULT_QUEUE_NAMING = "DEFAULT";

    private static final CounterConfiguration CONFIGURATION = CounterConfiguration
            .builder(CounterType.UNBOUNDED_STRONG)
            .initialValue(0)
            .storage(Storage.VOLATILE)
            .build();

    private final CounterManager counterManager;

    /** Cache of resolved counter name -> blocking counter handle (one per queue per JVM). */
    private final Map<String, SyncStrongCounter> counters = new ConcurrentHashMap<>();

    public StrongCounterRunningTracker(CounterManager counterManager) {
        this.counterManager = counterManager;
    }

    private String resolveName(String queue) {
        if (queue == null) {
            return Constants.RUNNING_COUNTER_KEY;
        } else if (queue.isBlank()) {
            throw new IllegalArgumentException("Counter key must not be blank");
        }
        return Constants.RUNNING_COUNTER_KEY + Constants.NAME_SEPARATOR + queue;
    }

    private SyncStrongCounter counter(String queue) {
        return counters.computeIfAbsent(resolveName(queue), name -> {
            // idempotent: returns false if the counter already exists cluster-wide
            counterManager.defineCounter(name, CONFIGURATION);
            return counterManager.getStrongCounter(name).sync();
        });
    }

    @Override
    public long get(@Nullable String queue) {
        return counter(queue).getValue();
    }

    @Override
    @Nullable
    public Long peek(@Nullable String queue) {
        String name = resolveName(queue);
        if (!counterManager.isDefined(name)) {
            return null;
        }
        return counter(queue).getValue();
    }

    @Override
    public long reserve(@Nullable String queue, long n) {
        return counter(queue).addAndGet(n);
    }

    @Override
    public void release(@Nullable String queue, long n) {
        addFloored(queue, -n);
    }

    @Override
    public void decrement(@Nullable String queue) {
        addFloored(queue, -1);
    }

    /** Add {@code delta} but never let the counter settle below zero (self-heals under-count). */
    private void addFloored(String queue, long delta) {
        SyncStrongCounter counter = counter(queue);
        long newValue = counter.addAndGet(delta);
        if (newValue < 0) {
            counter.addAndGet(-newValue);
            log.warn("QUEUE '{}': Running counter went below zero ({}); floored to 0.",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue, newValue);
        }
    }

    @Override
    public void reconcile(@Nullable String queue, long actual) {
        SyncStrongCounter counter = counter(queue);
        long current = counter.getValue();
        if (current != actual) {
            log.info("QUEUE '{}': Synchronizing running counter. Mismatch between active tasks and counter found. "
                            + "Previous value '{}' -> new value '{}'",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue, current, actual);
            counter.addAndGet(actual - current);
        }
    }

    @Override
    public void initialize(@Nullable String queue, long value) {
        reconcile(queue, value);
    }

    @Override
    public Set<String> queues() {
        Set<String> queues = new HashSet<>();
        for (String name : counterManager.getCounterNames()) {
            if (!name.startsWith(Constants.RUNNING_COUNTER_KEY)) {
                continue;
            }
            String suffix = name.substring(Constants.RUNNING_COUNTER_KEY.length());
            if (suffix.isEmpty()) {
                queues.add(null); // DEFAULT queue
            } else if (suffix.startsWith(Constants.NAME_SEPARATOR)) {
                queues.add(suffix.substring(Constants.NAME_SEPARATOR.length()));
            }
        }
        return queues;
    }

    @Override
    public boolean requiresReservation() {
        return true;
    }
}
