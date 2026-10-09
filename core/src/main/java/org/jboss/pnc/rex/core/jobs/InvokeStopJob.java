/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import java.util.HashMap;
import java.util.Set;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.pnc.api.dto.ErrorResponse;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.core.RemoteEntityClient;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;

public class InvokeStopJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final TaskController controller;

    private final ObjectMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStopJob.class);

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    protected void onException(Throwable e) {
        logger.error("STOP " + context.getName() + ": UNEXPECTED exception has been thrown.", e);
        Uni.createFrom()
                .voidItem()
                .onItem()
                .invoke(
                        (ignore) -> controller
                                .fail(context.getName(), createResponse(e), Origin.REX_INTERNAL_ERROR, false, Set.of()))
                .onFailure()
                .invoke(
                        (throwable) -> logger.warn(
                                "STOP " + context.getName()
                                        + ": Failed to transition task to STOP_FAILED state. Retrying.",
                                throwable))
                .onFailure()
                .retry()
                .atMost(5)
                .onFailure()
                .recoverWithNull()
                .await()
                .indefinitely();
    }

    private Object createResponse(Throwable e) {
        return mapper.convertValue(
                new ErrorResponse(e, "Rex couldn't invoke cancel on the remote entity."),
                HashMap.class);
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
    protected void onFailure() {
    }
}
