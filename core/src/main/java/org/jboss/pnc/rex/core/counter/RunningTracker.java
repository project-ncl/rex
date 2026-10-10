/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Tracks the number of currently running Tasks per queue (the "running counter").
 * <p>
 * This is an abstraction over the storage/concurrency mechanism of the running counter so the
 * implementation can be swapped (e.g. optimistic compare-and-swap on a cache entry vs. an
 * Infinispan clustered {@code StrongCounter}) without touching the {@code QueueManager} logic.
 * <p>
 * A {@code null} queue name refers to the DEFAULT queue.
 */
public interface RunningTracker {

    /**
     * Current number of running Tasks in the queue. For the DEFAULT queue the counter is lazily
     * initialized to {@code 0} if it does not exist yet.
     */
    long get(@Nullable String queue);

    /**
     * Current value without lazily creating it. Returns {@code null} if the queue's counter does
     * not exist (used by the admin/REST getter to distinguish a missing named queue).
     */
    @Nullable
    Long peek(@Nullable String queue);

    /**
     * Atomically add {@code n} to the running counter (reserve capacity for {@code n} starting
     * Tasks) and return the new value.
     *
     * @throws java.util.ConcurrentModificationException if the counter was modified concurrently
     *         (CAS-based implementations use this to trigger the outer transaction retry)
     */
    long reserve(@Nullable String queue, long n);

    /**
     * Atomically subtract {@code n} from the running counter (compensation for capacity that was
     * reserved but not consumed).
     */
    void release(@Nullable String queue, long n);

    /**
     * Decrease the running counter by one. Invoked when a Task leaves the RUNNING state group.
     */
    void decrement(@Nullable String queue);

    /**
     * Set the running counter of the queue to {@code actual} if it currently differs (drift
     * reconciliation against the real number of running Tasks).
     */
    void reconcile(@Nullable String queue, long actual);

    /**
     * Initialize the running counter of a (new) queue to {@code value}.
     */
    void initialize(@Nullable String queue, long value);

    /**
     * Names of all known queues that have a running counter. The DEFAULT queue is represented by
     * {@code null}.
     */
    Set<String> queues();
}
