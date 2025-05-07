/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
