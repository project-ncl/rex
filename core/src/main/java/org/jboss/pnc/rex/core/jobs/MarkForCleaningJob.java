/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Job to mark a task as disposable.
 */
public class MarkForCleaningJob extends ControllerJob {

    private final TaskController controller;

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final boolean pokeCleaner;

    public MarkForCleaningJob(Task context, boolean pokeCleaner) {
        super(INVOCATION_PHASE, context, false);
        this.pokeCleaner = pokeCleaner;
        this.controller = CDI.current().select(TaskController.class).get();
    }

    public MarkForCleaningJob(Task context) {
        this(context, false);
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        controller.markForDisposal(context.getName(), pokeCleaner);
        return true;
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
