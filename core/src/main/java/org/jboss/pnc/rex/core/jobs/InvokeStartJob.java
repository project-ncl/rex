/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

import io.smallrye.mutiny.Uni;
import org.jboss.pnc.rex.core.RemoteEntityClient;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class InvokeStartJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final TaskController controller;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStartJob.class);

    public InvokeStartJob(Task task) {
        super(INVOCATION_PHASE, task);
        this.client = CDI.current().select(RemoteEntityClient.class).get();
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        logger.info("START {}: STARTING", context.getName());
        client.startJob(context);
        return true;
    }

    @Override
    void onException(Throwable e) {
        logger.error("STOP " + context.getName() + ": UNEXPECTED exception has been thrown.", e);
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> controller.fail(context.getName(), "START : System failure. Exception: " + e.toString()))
                .onFailure().invoke((throwable) -> logger.warn("START " + context.getName() + ": Failed to transition task to START_FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }

}