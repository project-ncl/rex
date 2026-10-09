/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.core.delegates.WithRetries;

public class PokeQueueJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final QueueManager manager;

    public PokeQueueJob() {
        super(INVOCATION_PHASE, null, true);
        this.manager = CDI.current().select(QueueManager.class, () -> WithRetries.class).get();
    }

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    public boolean execute() {
        manager.poke();
        return true;
    }

    @Override
    protected void onException(Throwable e) {
    }

    @Override
    protected void onFailure() {
    }
}
