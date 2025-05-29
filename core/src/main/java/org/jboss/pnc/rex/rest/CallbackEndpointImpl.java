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
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.parameters.ErrorOption;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Slf4j
@ApplicationScoped
public class CallbackEndpointImpl implements CallbackEndpoint {

    private final TaskProvider taskProvider;

    // hacky self-delegate to trigger CDI interceptors (f.e. fault tolerance)
    private final CallbackEndpointImpl self;

    @Inject
    public CallbackEndpointImpl(TaskProvider provider, CallbackEndpointImpl self) {
        this.taskProvider = provider;
        this.self = self;
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    @Deprecated
    public void finish(String taskName, FinishRequest result, ErrorOption errorOption) {
        try {
            self.finishInternal(taskName, result);
        } catch (TaskMissingException e) {
            handleWithErrorOption(errorOption, e);
        } catch (ArcUndeclaredThrowableException e) {
            self.systemFailure(taskName, false);
        }
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void succeed(String taskName, Object result, ErrorOption errorOption) {
        try {
            self.succeedInternal(taskName, result, false);
        } catch (TaskMissingException e) {
            handleWithErrorOption(errorOption, e);
        } catch (ArcUndeclaredThrowableException e) {
            self.systemFailure(taskName, false);
        }
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void fail(String taskName, Object result, ErrorOption errorOption) {
        try {
            self.failInternal(taskName, result, false);
        } catch (TaskMissingException e) {
            handleWithErrorOption(errorOption, e);
        } catch (ArcUndeclaredThrowableException e) {
            self.systemFailure(taskName, false);
        }
    }

    @ApplyFaultTolerance("internal-retry")
    void failInternal(String taskName, Object result, boolean rollback) {
        taskProvider.acceptRemoteResponse(taskName, false, rollback, result);
    }

    @ApplyFaultTolerance("internal-retry")
    void succeedInternal(String taskName, Object result, boolean rollback) {
        taskProvider.acceptRemoteResponse(taskName, true, rollback, result);
    }

    @ApplyFaultTolerance("internal-retry")
    void finishInternal(String taskName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(taskName, result.getStatus(), false, result.getResponse());
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void rollbackOK(String taskName, Object result, ErrorOption err) {
        try {
            self.succeedInternal(taskName, result, true);
        } catch (TaskMissingException e) {
            handleWithErrorOption(err, e);
        } catch (ArcUndeclaredThrowableException e) {
            self.systemFailure(taskName, true);
        }
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void rollbackNOK(String taskName, Object result, ErrorOption err) {
        try {
            self.failInternal(taskName, result, true);
        } catch (TaskMissingException e) {
            handleWithErrorOption(err, e);
        } catch (ArcUndeclaredThrowableException e) {
            self.systemFailure(taskName, true);
        }
    }

    @Override
    public void beat(String taskName, Object body) {
        taskProvider.beat(taskName, body);
    }

    @ApplyFaultTolerance("internal-retry")
    void systemFailure(String taskName, boolean rollback) {
        log.error("STOP {}: UNEXPECTED exception has been thrown.", taskName);
        taskProvider.acceptRemoteResponse(taskName, false, rollback, "ACCEPT : System failure.");
    }

    void handleWithErrorOption(ErrorOption errorOption, RuntimeException e) {
        switch (errorOption) {
            case IGNORE -> {}
            case PASS_ERROR -> throw e;
        }
    }
}
