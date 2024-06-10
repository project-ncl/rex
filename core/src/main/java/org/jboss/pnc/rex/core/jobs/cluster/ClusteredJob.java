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
package org.jboss.pnc.rex.core.jobs.cluster;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.core.api.ClusteredJobManager;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.Task;

import java.util.HashMap;

import static org.jboss.pnc.rex.core.utils.OTELUtils.getOTELContext;

/**
 * The classes that implement this interface will be handled as ClusteredJobs, therefore, they will be failed-over in
 * case the node is unresponsive or shutdown (forcefully or gracefully).
 *
 * There are several restrictions and rules the implementors must obey:
 *  1. The #execute() part must be fully independent. It does not need any prior information apart from the type of Job
 *     and Task name.
 *  2. It has to expect that the underlying Task can be deleted at any time.
 *  3. It has to expect that the owner of this Job can change at any time. For the task it means it has to call
 *     #isOwned() at every deciding moment (for example right at the start of @Retries or if there is a long time
 *     period)
 *  4. It had to expect that the Task context may be outdated/inconsistent.
 *  5. The job has to be able to be run at ANY instance at ANY time.
 *  6. The job can be FULLY instantiated from its ClusteredJobReference. (Most of the required data should be in Task)
 *  7. Every unique ClusteredJob has its own CJobOperationType.
 *  8. The job has to check that the Task after consistency refresh is in the correct state for this Task to be run.
 */
public abstract class ClusteredJob extends ControllerJob {

    private static final TransactionPhase TRANSACTION_PHASE = TransactionPhase.AFTER_SUCCESS;

    protected final ClusteredJobManager manager;

    protected final ClusteredJobReference reference;
    private final ApplicationConfig config;
    private final CJobOperation operationType;
    private boolean avoidDelisting = false;

    // if created by TaskController
    protected ClusteredJob(Task context, CJobOperation operationType) {
        super(TRANSACTION_PHASE, context, true);
        this.manager = CDI.current().select(ClusteredJobManager.class).get();
        this.config = CDI.current().select(ApplicationConfig.class).get();
        this.operationType = operationType;
        this.reference = generateReference(context, operationType, config.name());
    }

    private ClusteredJobReference generateReference(Task context, CJobOperation operationType, String localInstanceName) {
        return new ClusteredJobReference(generateReferenceId(context.getName(), operationType),
            localInstanceName,
            operationType,
            new HashMap<>(getOTELContext()),
            context.getName());
    }

    // if created from reference
    protected ClusteredJob(ClusteredJobReference reference, CJobOperation operationType) {
        super(TRANSACTION_PHASE, null, true);
        this.operationType = operationType;

        this.manager = CDI.current().select(ClusteredJobManager.class).get();
        this.config = CDI.current().select(ApplicationConfig.class).get();
        this.context = CDI.current().select(TaskRegistry.class).get().getTask(reference.getTaskName()); // refresh context

        validateReference(reference);
        this.reference = reference;
    }

    private void validateReference(ClusteredJobReference reference) {
        if (reference == null || !reference.isOwnedBy(config.name()) || reference.getType() != operationType) {
            throw new IllegalArgumentException("Creating "+ this.getClass().getCanonicalName() + " from invalid reference: " + reference);
        }
    }

    public static String generateReferenceId(String taskName, CJobOperation operation) {
        return taskName+ '-' + operation.name();
    }

    /**
     * Enlist just in cases when the Job does not exist yet because it can be already persisted if the c-job was moved
     * from another instance to this one.
     *
     * @param reference
     * @return
     */
    private boolean shouldEnlist(ClusteredJobReference reference) {
        return !manager.exists(reference.getId());
    }

    @Override
    protected void beforeExecute() {
        QuarkusTransaction.joiningExisting().run(() -> {
            if (shouldEnlist(reference)) {
                manager.enlist(reference);
            }
        });
    }

    @Override
    protected void afterExecute() {
        QuarkusTransaction.joiningExisting().run(() -> {
            if (shouldDelist(reference)) {
                manager.delist(reference.getId());
            }
        });
    }

    @Override
    protected void onException(Throwable e) {
        // avoid de-listing if the thread is interrupted like in cases of shutting down
        if (e instanceof InterruptedException || e instanceof Error) {
            this.avoidDelisting = true;
        }
    }

    /**
     * Delist just in cases when this node instance is the actual owner of the Job. The Job may have been moved from
     * this node to another (by accident, maybe failed health-checks or a forced move) so the owner could have changed
     * if the Job takes longer time to finish.
     *
     * @param reference
     * @return
     */
    private boolean shouldDelist(ClusteredJobReference reference) {
        return !avoidDelisting && manager.isOwned(reference.getId());
    }

    protected boolean isOwned() {
        return QuarkusTransaction.joiningExisting().call(() -> manager.isOwned(reference.getId()));
    }

    @Override
    abstract public boolean execute();

    @Override
    protected void onFailure() {}
}
