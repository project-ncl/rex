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

import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;

import jakarta.enterprise.context.ApplicationScoped;

@Slf4j
@ApplicationScoped
public class QueueEndpointImpl implements QueueEndpoint {

    private final OptionsProvider optionsProvider;

    private final QueueManager queue;

    public QueueEndpointImpl(OptionsProvider optionsProvider, QueueManager queue) {
        this.optionsProvider = optionsProvider;
        this.queue = queue;
    }

    @Override
    @Retry
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-users-admin" })
    public void setConcurrent(Long amount) {
        optionsProvider.setConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return optionsProvider.getConcurrency();
    }

    @Override
    public LongResponse getRunning() {
        return LongResponse.builder().number(queue.getRunningCounter()).build();
    }
}
