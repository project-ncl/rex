/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.counter;

import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.pnc.rex.core.counter.CasRunningTracker;
import org.jboss.pnc.rex.core.counter.Counter;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Quarkus/Infinispan) for {@link CasRunningTracker}: verifies it maps the
 * high-level {@link org.jboss.pnc.rex.core.counter.RunningTracker} operations onto the underlying
 * optimistic compare-and-swap {@link Counter} primitives, including conflict handling.
 */
class CasRunningTrackerTest {

    private static VersionedValue<Long> vv(long value, long version) {
        return new VersionedValue<>() {
            @Override
            public long getVersion() {
                return version;
            }

            @Override
            public Long getValue() {
                return value;
            }
        };
    }

    @Test
    void getReturnsCurrentValue() {
        Counter counter = mock(Counter.class);
        when(counter.getMetadataValue("q")).thenReturn(vv(3L, 1L));

        assertThat(new CasRunningTracker(counter).get("q")).isEqualTo(3L);
    }

    @Test
    void getReturnsZeroWhenCounterMissing() {
        Counter counter = mock(Counter.class);
        when(counter.getMetadataValue("q")).thenReturn(null);

        assertThat(new CasRunningTracker(counter).get("q")).isZero();
    }

    @Test
    void peekReturnsNullWhenCounterMissing() {
        Counter counter = mock(Counter.class);
        when(counter.getMetadataValue(null)).thenReturn(null);

        assertThat(new CasRunningTracker(counter).peek(null)).isNull();
    }

    @Test
    void peekReturnsValueWhenPresent() {
        Counter counter = mock(Counter.class);
        when(counter.getMetadataValue("q")).thenReturn(vv(7L, 2L));

        assertThat(new CasRunningTracker(counter).peek("q")).isEqualTo(7L);
    }

    @Test
    void reserveAddsAmountAndReturnsNewValue() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(2L, 5L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue("q", meta, 5L)).thenReturn(true); // 2 + 3

        long result = new CasRunningTracker(counter).reserve("q", 3);

        assertThat(result).isEqualTo(5L);
        verify(counter).replaceValue("q", meta, 5L);
    }

    @Test
    void reserveThrowsConcurrentModificationOnVersionConflict() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(2L, 5L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue(eq("q"), eq(meta), any())).thenReturn(false);

        assertThatThrownBy(() -> new CasRunningTracker(counter).reserve("q", 1))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void releaseSubtractsAmount() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(5L, 9L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue("q", meta, 2L)).thenReturn(true); // 5 - 3

        new CasRunningTracker(counter).release("q", 3);

        verify(counter).replaceValue("q", meta, 2L);
    }

    @Test
    void decrementSubtractsOne() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(5L, 9L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue("q", meta, 4L)).thenReturn(true);

        new CasRunningTracker(counter).decrement("q");

        verify(counter).replaceValue("q", meta, 4L);
    }

    @Test
    void decrementThrowsConcurrentModificationOnVersionConflict() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(5L, 9L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue(eq("q"), eq(meta), any())).thenReturn(false);

        assertThatThrownBy(() -> new CasRunningTracker(counter).decrement("q"))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void reconcileWritesActualWhenDifferent() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(5L, 9L);
        when(counter.getMetadataValue("q")).thenReturn(meta);
        when(counter.replaceValue("q", meta, 2L)).thenReturn(true);

        new CasRunningTracker(counter).reconcile("q", 2);

        verify(counter).replaceValue("q", meta, 2L);
    }

    @Test
    void reconcileIsNoopWhenAlreadyEqual() {
        Counter counter = mock(Counter.class);
        VersionedValue<Long> meta = vv(4L, 9L);
        when(counter.getMetadataValue("q")).thenReturn(meta);

        new CasRunningTracker(counter).reconcile("q", 4);

        verify(counter, never()).replaceValue(anyString(), any(VersionedValue.class), any());
    }

    @Test
    void reconcileInitializesWhenCounterMissing() {
        Counter counter = mock(Counter.class);
        when(counter.getMetadataValue("q")).thenReturn(null);

        new CasRunningTracker(counter).reconcile("q", 6);

        verify(counter).initialize("q", 6L);
    }

    @Test
    void initializeDelegatesToCounter() {
        Counter counter = mock(Counter.class);

        new CasRunningTracker(counter).initialize("q", 0L);

        verify(counter).initialize("q", 0L);
    }

    @Test
    void queuesReturnsCounterKeysIncludingDefaultNull() {
        Counter counter = mock(Counter.class);
        Map<String, Long> entries = new LinkedHashMap<>();
        entries.put(null, 1L); // DEFAULT queue
        entries.put("vm", 2L);
        when(counter.entries()).thenReturn(entries);

        assertThat(new CasRunningTracker(counter).queues()).containsExactlyInAnyOrder(null, "vm");
    }
}
