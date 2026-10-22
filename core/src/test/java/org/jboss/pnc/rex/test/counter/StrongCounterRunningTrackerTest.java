/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.counter;

import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.SyncStrongCounter;
import org.jboss.pnc.rex.core.counter.StrongCounterRunningTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Quarkus/Infinispan server) for {@link StrongCounterRunningTracker}: verifies it
 * maps the {@link org.jboss.pnc.rex.core.counter.RunningTracker} operations onto the Infinispan
 * clustered StrongCounter API, including the non-negative floor and queue-name parsing.
 */
class StrongCounterRunningTrackerTest {

    private static final String RUNNING = "RUNNING";

    /** Wires a CounterManager mock whose counters are all backed by the given SyncStrongCounter. */
    private static CounterManager counterManagerReturning(SyncStrongCounter sync) {
        CounterManager cm = mock(CounterManager.class);
        StrongCounter strong = mock(StrongCounter.class);
        when(cm.defineCounter(anyString(), any(CounterConfiguration.class))).thenReturn(true);
        when(cm.getStrongCounter(anyString())).thenReturn(strong);
        when(strong.sync()).thenReturn(sync);
        return cm;
    }

    @Test
    void getReturnsCounterValue() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.getValue()).thenReturn(3L);

        assertThat(new StrongCounterRunningTracker(counterManagerReturning(sync)).get("q")).isEqualTo(3L);
    }

    @Test
    void defaultQueueUsesPlainRunningCounterName() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        CounterManager cm = counterManagerReturning(sync);

        new StrongCounterRunningTracker(cm).get(null);

        verify(cm).getStrongCounter(RUNNING);
    }

    @Test
    void namedQueueUsesSuffixedCounterName() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        CounterManager cm = counterManagerReturning(sync);

        new StrongCounterRunningTracker(cm).get("vm");

        verify(cm).getStrongCounter("RUNNING-vm");
    }

    @Test
    void peekReturnsNullWhenCounterNotDefined() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        CounterManager cm = counterManagerReturning(sync);
        when(cm.isDefined("RUNNING-vm")).thenReturn(false);

        assertThat(new StrongCounterRunningTracker(cm).peek("vm")).isNull();
    }

    @Test
    void peekReturnsValueWhenCounterDefined() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.getValue()).thenReturn(7L);
        CounterManager cm = counterManagerReturning(sync);
        when(cm.isDefined("RUNNING-vm")).thenReturn(true);

        assertThat(new StrongCounterRunningTracker(cm).peek("vm")).isEqualTo(7L);
    }

    @Test
    void reserveAddsAmountAndReturnsNewValue() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.addAndGet(5L)).thenReturn(5L);

        long result = new StrongCounterRunningTracker(counterManagerReturning(sync)).reserve("q", 5);

        assertThat(result).isEqualTo(5L);
        verify(sync).addAndGet(5L);
    }

    @Test
    void releaseSubtractsAmount() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.addAndGet(-3L)).thenReturn(2L);

        new StrongCounterRunningTracker(counterManagerReturning(sync)).release("q", 3);

        verify(sync).addAndGet(-3L);
        verify(sync, never()).addAndGet(eq(3L)); // no floor correction needed
    }

    @Test
    void releaseFloorsAtZeroWhenGoingNegative() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.addAndGet(-3L)).thenReturn(-1L); // underflow
        when(sync.addAndGet(1L)).thenReturn(0L);   // correction back to zero

        new StrongCounterRunningTracker(counterManagerReturning(sync)).release("q", 3);

        verify(sync).addAndGet(-3L);
        verify(sync).addAndGet(1L);
    }

    @Test
    void decrementSubtractsOne() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.addAndGet(-1L)).thenReturn(4L);

        new StrongCounterRunningTracker(counterManagerReturning(sync)).decrement("q");

        verify(sync).addAndGet(-1L);
    }

    @Test
    void reconcileAdjustsByDeltaWhenDifferent() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.getValue()).thenReturn(5L);

        new StrongCounterRunningTracker(counterManagerReturning(sync)).reconcile("q", 2);

        verify(sync).addAndGet(-3L); // 2 - 5
    }

    @Test
    void reconcileIsNoopWhenAlreadyEqual() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        when(sync.getValue()).thenReturn(4L);

        new StrongCounterRunningTracker(counterManagerReturning(sync)).reconcile("q", 4);

        verify(sync, never()).addAndGet(anyLong());
    }

    @Test
    void queuesParsesCounterNamesAndMapsDefaultToNull() {
        SyncStrongCounter sync = mock(SyncStrongCounter.class);
        CounterManager cm = counterManagerReturning(sync);
        when(cm.getCounterNames()).thenReturn(List.of(RUNNING, "RUNNING-vm", "SOMETHING_ELSE"));

        assertThat(new StrongCounterRunningTracker(cm).queues())
                .containsExactlyInAnyOrder(null, "vm");
    }

    @Test
    void requiresReservationIsTrue() {
        assertThat(new StrongCounterRunningTracker(mock(CounterManager.class)).requiresReservation()).isTrue();
    }
}
