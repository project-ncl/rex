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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.pnc.api.dto.ErrorResponse;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.core.RemoteEntityClient;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class InvokeRollbackJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final TaskController controller;

    private final ObjectMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(InvokeRollbackJob.class);

    public InvokeRollbackJob(Task task) {
        super(INVOCATION_PHASE, task, true);
        this.client = CDI.current().select(RemoteEntityClient.class).get();
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
        this.mapper = CDI.current().select(ObjectMapper.class).get();
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        logger.info("ROLLBACK {}: RESTORING", context.getName());
        client.rollbackJob(context);
        return true;
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {
        logger.error("ROLLBACK {}: UNEXPECTED exception has been thrown.", context.getName(), e);
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> controller.fail(context.getName(), createResponse(e), Origin.REX_INTERNAL_ERROR, true))
                .onFailure().invoke((throwable) -> logger.warn("ROLLBACK {}: Failed to transition task to START_FAILED state. Retrying.", context.getName(), throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }

    private Object createResponse(Throwable e) {
        return mapper.convertValue(
                new ErrorResponse(e, "Rex failed to rollback a Task on the remote entity."), HashMap.class);
    }
}
