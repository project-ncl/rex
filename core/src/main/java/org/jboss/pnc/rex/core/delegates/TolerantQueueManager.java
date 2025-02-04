/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core.delegates;

import io.quarkus.arc.Unremovable;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
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
    @ApplyFaultTolerance("internal-retry")
    public void poke() {
        delegate.poke();
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    public void decreaseRunningCounter(String name) {
        delegate.decreaseRunningCounter(name);
    }

    @Override
    public void setMaximumConcurrency(String name, Long amount) {
        delegate.setMaximumConcurrency(name, amount);
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    public Long getMaximumConcurrency(String name) {
        return delegate.getMaximumConcurrency(name);
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    public void synchronizeRunningCounter() {
        delegate.synchronizeRunningCounter();
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    public Long getRunningCounter(String name) {
        return delegate.getRunningCounter(name);
    }
}
