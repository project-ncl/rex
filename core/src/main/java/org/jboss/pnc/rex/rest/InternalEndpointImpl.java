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

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;
import org.jboss.pnc.rex.facade.api.TaskProvider;
import org.jboss.pnc.rex.rest.api.InternalEndpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

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
    @Retry(maxRetries = 15,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public void finish(String taskName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(taskName, result.getStatus(), result.getResponse());
    }

    @Override
    @Retry(maxRetries = 5,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public void setConcurrent(Long amount) {
        optionsProvider.setConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return optionsProvider.getConcurrency();
    }
}
