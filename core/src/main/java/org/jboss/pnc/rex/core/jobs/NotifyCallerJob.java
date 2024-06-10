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

import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.CallerNotificationClient;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

public class NotifyCallerJob extends ControllerJob {

    private static final Logger log = LoggerFactory.getLogger(NotifyCallerJob.class);

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final Transition transition;

    private final CallerNotificationClient client;

    public NotifyCallerJob(Transition transition, Task task) {
        super(INVOCATION_PHASE, task, true);
        this.transition = transition;
        this.client = CDI.current().select(CallerNotificationClient.class).get();
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        return client.notifyCaller(transition, context);
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {
        log.error("NOTIFICATION {}: UNEXPECTED exception has been thrown.", context.getName(), e);
    }

    public Transition getTransition() {
        return transition;
    }
}
