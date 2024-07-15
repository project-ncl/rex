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
package org.jboss.pnc.rex.core.jobs;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Task;

public class ClearConstraintJob extends ControllerJob {

    private final TaskController controller;

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final FaultToleranceDecorator tolerance;

    public ClearConstraintJob(Task context) {
        super(INVOCATION_PHASE, context, false);
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
        this.tolerance = CDI.current().select(FaultToleranceDecorator.class).get();
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        tolerance.withTolerance(() -> controller.clearConstraint(context.getName()));
        return true;
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
