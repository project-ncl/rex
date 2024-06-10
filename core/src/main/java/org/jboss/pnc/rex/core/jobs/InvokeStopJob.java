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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.jboss.pnc.api.dto.ErrorResponse;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.core.RemoteEntityClient;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import java.util.HashMap;

public class InvokeStopJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final TaskController controller;

    private final ObjectMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStopJob.class);

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    protected void onException(Throwable e) {
        logger.error("STOP " + context.getName() + ": UNEXPECTED exception has been thrown.", e);
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> controller.fail(context.getName(), createResponse(e), Origin.REX_INTERNAL_ERROR))
                .onFailure().invoke((throwable) -> logger.warn("STOP " + context.getName() + ": Failed to transition task to STOP_FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }

    private Object createResponse(Throwable e) {
        return mapper.convertValue(
                new ErrorResponse(e, "Rex couldn't invoke cancel on the remote entity."), HashMap.class);
    }

    public InvokeStopJob(Task task) {
        super(INVOCATION_PHASE, task, true);
        this.client = CDI.current().select(RemoteEntityClient.class).get();
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
        this.mapper = CDI.current().select(ObjectMapper.class).get();

    }

    @Override
    public boolean execute() {
        logger.info("STOP {}: STOPPING", context.getName());
        client.stopJob(context);
        return true;
    }

    @Override
    protected void onFailure() {}
}
