/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.core.api.CleaningManager;
import org.jboss.pnc.rex.core.delegates.WithRetries;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

public class PokeCleanJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final CleaningManager manager;

    public PokeCleanJob() {
        super(INVOCATION_PHASE, null, false);
        this.manager = CDI.current().select(CleaningManager.class, () -> WithRetries.class).get();
    }

    @Override
    protected void beforeExecute() {

    }

    @Override
    protected void afterExecute() {

    }

    @Override
    public boolean execute() {
        manager.tryClean();
        return true;
    }

    @Override
    protected void onException(Throwable e) {}

    @Override
    protected void onFailure() {}
}
