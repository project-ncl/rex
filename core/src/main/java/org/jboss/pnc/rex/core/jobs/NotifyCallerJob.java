/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import java.util.TreeSet;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.CallerNotificationClient;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyCallerJob extends ControllerJob {

    private static final Logger log = LoggerFactory.getLogger(NotifyCallerJob.class);

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final Transition transition;

    private final CallerNotificationClient client;

    public NotifyCallerJob(Transition transition, Task task) {
        super(INVOCATION_PHASE, bestEffortCopy(task), true);
        this.transition = transition;
        this.client = CDI.current().select(CallerNotificationClient.class).get();
    }

    private static Task bestEffortCopy(Task task) {
        return task.toBuilder()
                .timestamps(new TreeSet<>(task.getTimestamps()))
                //                .serverResponses(new ArrayList<>(task.getServerResponses()))
                .rollbackMeta(task.getRollbackMeta() != null ? task.getRollbackMeta().toBuilder().build() : null)
                .configuration(task.getConfiguration() != null ? task.getConfiguration().toBuilder().build() : null)
                .build();
    }

    @Override
    protected void beforeExecute() {
    }

    @Override
    protected void afterExecute() {
    }

    @Override
    public boolean execute() {
        return client.notifyCaller(transition, context);
    }

    @Override
    protected void onFailure() {
    }

    @Override
    protected void onException(Throwable e) {
        log.error("NOTIFICATION {}: UNEXPECTED exception has been thrown.", context.getName(), e);
    }

    public Transition getTransition() {
        return transition;
    }
}
