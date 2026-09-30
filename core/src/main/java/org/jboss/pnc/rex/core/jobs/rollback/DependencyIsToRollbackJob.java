/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs.rollback;

import jakarta.enterprise.event.TransactionPhase;
import org.jboss.pnc.rex.core.jobs.DependantMessageJob;
import org.jboss.pnc.rex.model.Task;

public class DependencyIsToRollbackJob extends DependantMessageJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencyIsToRollbackJob(Task task) {
        super(task, INVOCATION_PHASE);
    }

    @Override
    protected void inform(String dependentName) {
        dependentAPI.dependencyIsToRollback(dependentName);
    }
}
