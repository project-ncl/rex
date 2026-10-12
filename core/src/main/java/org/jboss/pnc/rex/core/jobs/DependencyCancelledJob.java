/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import jakarta.enterprise.event.TransactionPhase;

import org.jboss.pnc.rex.model.Task;

public class DependencyCancelledJob extends DependantMessageJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final String cause;

    public DependencyCancelledJob(Task task, String cause) {
        super(task, INVOCATION_PHASE);
        this.cause = cause;
    }

    @Override
    protected void inform(String dependentName) {
        dependentAPI.dependencyCancelled(dependentName, cause);
    }
}
