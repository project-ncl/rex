/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.counter.api.CounterManager;

/**
 * Selects the active {@link RunningTracker} implementation based on configuration.
 * <p>
 * {@code rex.queue.running-tracker}:
 * <ul>
 *     <li>{@code cas} (default) - optimistic compare-and-swap on the {@code rex-counter} cache
 *         (historical behavior, {@link CasRunningTracker});</li>
 *     <li>{@code strong} - Infinispan clustered StrongCounter ({@link StrongCounterRunningTracker}),
 *         which removes the single-key contention and scales concurrency.</li>
 * </ul>
 * Defaulting to {@code cas} means this can be shipped dark and flipped per environment.
 */
@Slf4j
@ApplicationScoped
public class RunningTrackerProducer {

    public static final String CONFIG_KEY = "rex.queue.running-tracker";
    public static final String STRONG = "strong";
    public static final String CAS = "cas";

    @Produces
    @ApplicationScoped
    public RunningTracker runningTracker(@ConfigProperty(name = CONFIG_KEY, defaultValue = CAS) String impl,
                                         @Running Counter casCounter,
                                         CounterManager counterManager) {
        if (STRONG.equalsIgnoreCase(impl)) {
            log.info("QUEUE: Using StrongCounter running-counter tracker ('{}')", impl);
            return new StrongCounterRunningTracker(counterManager);
        }
        log.info("QUEUE: Using compare-and-swap running-counter tracker ('{}')", impl);
        return new CasRunningTracker(casCounter);
    }
}
