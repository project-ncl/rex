/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.core.api.DependencyMessenger;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Set;

public abstract class DependencyMessageJob extends ControllerJob {

    private final Set<String> dependencies;

    protected DependencyMessenger dependencyAPI;

    protected DependencyMessageJob(Task task, TransactionPhase invocationPhase) {
        super(invocationPhase, task, false);
        this.dependencies = task.getDependencies();
        this.dependencyAPI = CDI.current().select(DependencyMessenger.class).get();
    }

    @Override
    public boolean execute() {
        for (String dependent : dependencies) {
            inform(dependent);
        }
        return true;
    }

    protected abstract void inform(final String dependencyName);

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
