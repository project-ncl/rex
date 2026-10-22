/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.core.delegates.WithRetries;

/**
 * When the StrongCounter tracker is active, reconcile the (volatile) running counters from live task
 * state at startup. This restores correct values after a full-cluster restart that dropped the
 * counters while tasks survived in the persistent task cache. No-op for the default CAS tracker,
 * whose counters live in the persistent cache and survive restarts on their own.
 */
@Slf4j
@ApplicationScoped
public class RunningCounterStartupReconciler {

    private final String impl;
    private final QueueManager queueManager;

    public RunningCounterStartupReconciler(
            @ConfigProperty(name = RunningTrackerProducer.CONFIG_KEY, defaultValue = RunningTrackerProducer.CAS) String impl,
            @WithRetries QueueManager queueManager) {
        this.impl = impl;
        this.queueManager = queueManager;
    }

    @Startup(ObserverMethod.DEFAULT_PRIORITY + 2)
    void reconcileOnStartup() {
        if (!RunningTrackerProducer.STRONG.equalsIgnoreCase(impl)) {
            return;
        }
        try {
            log.info("QUEUE: Reconciling running counters from task state on startup");
            QuarkusTransaction.requiringNew().run(queueManager::synchronizeRunningCounter);
        } catch (RuntimeException e) {
            log.warn("QUEUE: Startup running-counter reconciliation failed; it will be corrected by "
                    + "on-demand/maintenance reconciliation", e);
        }
    }
}
