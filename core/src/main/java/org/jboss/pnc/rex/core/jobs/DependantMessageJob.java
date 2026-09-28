/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.core.api.DependentMessenger;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Set;

/**
 * Jobs implementing this abstract class are used for messaging all dependants about important changes(dependency has
 * finished, failed...)
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public abstract class DependantMessageJob extends ControllerJob {

    private final Set<String> dependents;

    protected DependentMessenger dependentAPI;

    protected DependantMessageJob(Task task, TransactionPhase invocationPhase) {
        super(invocationPhase, task, false);
        this.dependents = task.getDependants();
        this.dependentAPI = CDI.current().select(DependentMessenger.class).get();
    }

    @Override
    public boolean execute() {
        for (String dependent : dependents) {
            inform(dependent);
        }
        return true;
    }

    protected abstract void inform(final String dependentName);

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
