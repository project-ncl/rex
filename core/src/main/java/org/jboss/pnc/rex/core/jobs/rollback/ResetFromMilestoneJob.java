/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs.rollback;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.model.Task;

public class ResetFromMilestoneJob extends ControllerJob {

    private final TaskController controller;
    private final FaultToleranceDecorator ft;

    public ResetFromMilestoneJob(Task context) {
        super(TransactionPhase.AFTER_SUCCESS, context, true);
        this.ft = CDI.current().select(FaultToleranceDecorator.class).get();
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
    }

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    public boolean execute() {
        ft.withTolerance(() -> controller.reset(context.getName()));
        return true;
    }

    @Override
    protected void onFailure() {
    }

    @Override
    protected void onException(Throwable e) {
    }
}
