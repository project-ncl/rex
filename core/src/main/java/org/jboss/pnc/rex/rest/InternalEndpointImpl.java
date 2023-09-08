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
package org.jboss.pnc.rex.rest;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.api.InternalEndpoint;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;
import org.jboss.pnc.rex.facade.api.TaskProvider;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;

@Slf4j
@ApplicationScoped
public class InternalEndpointImpl implements InternalEndpoint {

    private final TaskProvider taskProvider;

    private final OptionsProvider optionsProvider;

    @Inject
    public InternalEndpointImpl(TaskProvider provider, OptionsProvider optionsProvider) {
        this.taskProvider = provider;
        this.optionsProvider = optionsProvider;
    }

    @Override
    @Retry
    @Fallback(fallbackMethod = "fallback", applyOn = {RollbackException.class, ArcUndeclaredThrowableException.class})
    @RolesAllowed({ "pnc-users-rex-admin", "pnc-users-rex-user", "pnc-users-admin" })
    @Deprecated
    public void finish(String taskName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(taskName, result.getStatus(), result.getResponse());
    }

    @Override
    @Retry
    @Fallback(fallbackMethod = "objectFallback", applyOn = {RollbackException.class, ArcUndeclaredThrowableException.class})
    @RolesAllowed({ "pnc-users-rex-admin", "pnc-users-rex-user", "pnc-users-admin" })
    public void succeed(String taskName, Object result) {
        taskProvider.acceptRemoteResponse(taskName, true, result);
    }

    @Override
    @Retry
    @Fallback(fallbackMethod = "objectFallback", applyOn = {RollbackException.class, ArcUndeclaredThrowableException.class})
    @RolesAllowed({ "pnc-users-rex-admin", "pnc-users-rex-user", "pnc-users-admin" })
    public void fail(String taskName, Object result) {
        taskProvider.acceptRemoteResponse(taskName, false, result);
    }

    @Override
    @Retry
    @RolesAllowed({ "pnc-users-rex-admin", "pnc-users-admin" })
    public void setConcurrent(Long amount) {
        optionsProvider.setConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return optionsProvider.getConcurrency();
    }

    // once https://github.com/smallrye/smallrye-fault-tolerance/issues/492 is merged, use FallbackHandler
    void fallback(String taskName, FinishRequest result) {
        log.error("STOP " + taskName + ": UNEXPECTED exception has been thrown.");
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> taskProvider.acceptRemoteResponse(taskName, false,"ACCEPT : System failure."))
                .onFailure().invoke((throwable) -> log.warn("ACCEPT " + taskName + ": Failed to transition task to FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }

    void objectFallback(String taskName, Object result) {
        log.error("STOP " + taskName + ": UNEXPECTED exception has been thrown.");
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> taskProvider.acceptRemoteResponse(taskName, false,"ACCEPT : System failure."))
                .onFailure().invoke((throwable) -> log.warn("ACCEPT " + taskName + ": Failed to transition task to FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }
}
