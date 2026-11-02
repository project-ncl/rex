/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.VersionedValue;

import java.util.ConcurrentModificationException;
import java.util.Set;

/**
 * Optimistic compare-and-swap implementation of {@link RunningTracker}, backed by the existing
 * {@code rex-counter} cache through the {@link Running} {@link Counter}.
 * <p>
 * This preserves the historical behavior: every mutation reads the versioned value and writes it
 * back with {@code replaceWithVersion}; on a version conflict it throws
 * {@link ConcurrentModificationException} so the surrounding {@code internal-retry} guarded
 * transaction is replayed.
 * <p>
 * Instantiated by {@link RunningTrackerProducer} (not a CDI bean itself) so the active
 * implementation can be selected by configuration.
 */
@Slf4j
public class CasRunningTracker implements RunningTracker {

    private static final String DEFAULT_QUEUE_NAMING = "DEFAULT";

    private final Counter counter;

    public CasRunningTracker(@Running Counter counter) {
        this.counter = counter;
    }

    @Override
    public long get(@Nullable String queue) {
        VersionedValue<Long> meta = counter.getMetadataValue(queue);
        return meta == null ? 0L : meta.getValue();
    }

    @Override
    @Nullable
    public Long peek(@Nullable String queue) {
        VersionedValue<Long> meta = counter.getMetadataValue(queue);
        return meta == null ? null : meta.getValue();
    }

    @Override
    public long reserve(@Nullable String queue, long n) {
        VersionedValue<Long> meta = counter.getMetadataValue(queue);
        long newValue = meta.getValue() + n;
        if (!counter.replaceValue(queue, meta, newValue)) {
            throw concurrentModification(queue);
        }
        return newValue;
    }

    @Override
    public void release(@Nullable String queue, long n) {
        VersionedValue<Long> meta = counter.getMetadataValue(queue);
        long newValue = meta.getValue() - n;
        if (!counter.replaceValue(queue, meta, newValue)) {
            throw concurrentModification(queue);
        }
    }

    @Override
    public void decrement(@Nullable String queue) {
        release(queue, 1);
    }

    @Override
    public void reconcile(@Nullable String queue, long actual) {
        VersionedValue<Long> meta = counter.getMetadataValue(queue);
        if (meta == null) {
            counter.initialize(queue, actual);
            return;
        }
        if (meta.getValue() != actual) {
            log.info("QUEUE '{}': Synchronizing running counter. Mismatch between active tasks and counter found. "
                            + "Previous value '{}' -> new value '{}'",
                    queue == null ? DEFAULT_QUEUE_NAMING : queue, meta.getValue(), actual);
            counter.replaceValue(queue, meta, actual);
        }
    }

    @Override
    public void initialize(@Nullable String queue, long value) {
        counter.initialize(queue, value);
    }

    @Override
    public Set<String> queues() {
        return counter.entries().keySet();
    }

    @Override
    public boolean requiresReservation() {
        // Optimistic CAS on the shared counter key already serializes admission across nodes;
        // keep the historical dequeue-then-increment order.
        return false;
    }

    private ConcurrentModificationException concurrentModification(String queue) {
        ConcurrentModificationException e =
                new ConcurrentModificationException("Running counter was modified concurrently.");
        log.error("QUEUE '{}': Concurrent modification detected.", queue == null ? DEFAULT_QUEUE_NAMING : queue, e);
        return e;
    }
}
