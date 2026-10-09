/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs.rollback;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.model.Task;

public class RollbackTriggeredJob extends ControllerJob {

    private final TaskController controller;

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public RollbackTriggeredJob(Task context) {
        super(INVOCATION_PHASE, context, false);
        this.controller = CDI.current().select(TaskController.class).get();
    }

    @Override
    public boolean execute() {
        controller.rollbackTriggered(context.getName());
        return true;
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void onFailure() {
    }

    @Override
    protected void onException(Throwable e) {
    }
}
