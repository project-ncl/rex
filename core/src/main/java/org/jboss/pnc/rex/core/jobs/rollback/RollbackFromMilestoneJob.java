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
import org.jboss.pnc.rex.core.api.RollbackManager;
import org.jboss.pnc.rex.core.delegates.WithRetries;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollbackFromMilestoneJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private static final Logger logger = LoggerFactory.getLogger(RollbackFromMilestoneJob.class);

    private final RollbackManager manager;

    public RollbackFromMilestoneJob(Task task) {
        super(INVOCATION_PHASE, task, true);
        this.manager = CDI.current().select(RollbackManager.class, () -> WithRetries.class).get();
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        logger.info("ROLLBACK {}: Initiating rollback process from Milestone {}", context.getName(), context.getMilestoneTask());
        manager.rollbackFromMilestone(context.getMilestoneTask());

        return true;
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
