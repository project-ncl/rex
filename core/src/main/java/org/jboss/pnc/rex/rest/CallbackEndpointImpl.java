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
package org.jboss.pnc.rex.rest;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.parameters.ErrorOption;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;

@Slf4j
@ApplicationScoped
public class CallbackEndpointImpl implements CallbackEndpoint {

    private final TaskProvider taskProvider;

    @Inject
    public CallbackEndpointImpl(TaskProvider provider) {
        this.taskProvider = provider;
    }

    @Override
    @Fallback(fallbackMethod = "fallback", applyOn = {RollbackException.class,
        ArcUndeclaredThrowableException.class,
        TaskMissingException.class
    })
    @Retry
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    @Deprecated
    public void finish(String taskName, FinishRequest result, ErrorOption errorOption) {
        taskProvider.acceptRemoteResponse(taskName, result.getStatus(), result.getResponse());
    }

    @Override
    @Retry
    @Fallback(fallbackMethod = "objectFallback", applyOn = {RollbackException.class, ArcUndeclaredThrowableException.class, TaskMissingException.class})
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void succeed(String taskName, Object result, ErrorOption errorOption) {
        taskProvider.acceptRemoteResponse(taskName, true, result);
    }

    @Override
    @Fallback(fallbackMethod = "objectFallback", applyOn = {RollbackException.class, ArcUndeclaredThrowableException.class, TaskMissingException.class})
    @Retry
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void fail(String taskName, Object result, ErrorOption errorOption) {
        taskProvider.acceptRemoteResponse(taskName, false, result);
    }

    /**
     * Return positive Status Code on ErrorOption.IGNORE if task is missing.
     */
    void fallback(String taskName, FinishRequest result, ErrorOption errorOption, TaskMissingException e) {
        handleWithErrorOption(errorOption, e);
    }

    void objectFallback(String taskName, Object result, ErrorOption errorOption, TaskMissingException e) {
        handleWithErrorOption(errorOption, e);
    }

    void fallback(String taskName, FinishRequest result, ErrorOption errorOption) {
        systemFailure(taskName);
    }

    void objectFallback(String taskName, Object result, ErrorOption errorOption) {
        systemFailure(taskName);
    }

    private void handleWithErrorOption(ErrorOption errorOption, RuntimeException e) {
        switch (errorOption) {
            case IGNORE -> {}
            case PASS_ERROR -> throw e;
        }
    }

    private void systemFailure(String taskName) {
        log.error("STOP " + taskName + ": UNEXPECTED exception has been thrown.");
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> taskProvider.acceptRemoteResponse(taskName, false,"ACCEPT : System failure."))
                .onFailure().invoke((throwable) -> log.warn("ACCEPT " + taskName + ": Failed to transition task to FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }
}
