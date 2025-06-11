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

import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.common.exceptions.QueueMissingException;
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
    @ApplyGuard("internal-retry")
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void setConcurrent(Long amount) {
        setConcurrentNamed(null, amount);
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public void setConcurrentNamed(String name, Long amount) {
        optionsProvider.setConcurrency(name, amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return getConcurrentNamed(null);
    }

    @Override
    public LongResponse getConcurrentNamed(String name) {
        return optionsProvider.getConcurrency(name);
    }

    @Override
    public LongResponse getRunning() {
        return getRunningNamed(null);
    }

    @Override
    public LongResponse getRunningNamed(String name) {
        Long runningCounter = getRunningCounter(name);
        if (runningCounter == null) {
            throw new QueueMissingException("Queue with name " + name + " not found.", name);
        }

        return LongResponse.builder().number(runningCounter).build();
    }

    private Long getRunningCounter(String name) {
        return queue.getRunningCounter(name);
    }
}
