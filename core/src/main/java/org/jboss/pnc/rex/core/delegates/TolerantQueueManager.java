/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.delegates;

import io.quarkus.arc.Unremovable;
import io.smallrye.faulttolerance.api.ApplyGuard;
import org.jboss.pnc.rex.core.api.QueueManager;

import jakarta.enterprise.context.ApplicationScoped;

@WithRetries
@Unremovable
@ApplicationScoped
public class TolerantQueueManager implements QueueManager {

    private final QueueManager delegate;

    public TolerantQueueManager(QueueManager manager) {
        this.delegate = manager;
    }

    @Override
    @ApplyGuard("internal-retry")
    public void poke() {
        delegate.poke();
    }

    @Override
    @ApplyGuard("internal-retry")
    public void decreaseRunningCounter(String name) {
        delegate.decreaseRunningCounter(name);
    }

    @Override
    public void setMaximumConcurrency(String name, Long amount) {
        delegate.setMaximumConcurrency(name, amount);
    }

    @Override
    @ApplyGuard("internal-retry")
    public Long getMaximumConcurrency(String name) {
        return delegate.getMaximumConcurrency(name);
    }

    @Override
    @ApplyGuard("internal-retry")
    public void synchronizeRunningCounter() {
        delegate.synchronizeRunningCounter();
    }

    @Override
    @ApplyGuard("internal-retry")
    public Long getRunningCounter(String name) {
        return delegate.getRunningCounter(name);
    }
}
