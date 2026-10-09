/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.model.Task;

public class DecreaseCounterJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final QueueManager queueManager;

    public DecreaseCounterJob(Task context) {
        super(INVOCATION_PHASE, context, false);
        this.queueManager = CDI.current().select(QueueManager.class).get();
    }

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    public boolean execute() {
        queueManager.decreaseRunningCounter(context.getQueue());
        return true;
    }

    @Override
    protected void onFailure() {
    }

    @Override
    protected void onException(Throwable e) {
    }
}
