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
package org.jboss.pnc.rex.facade;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.common.exceptions.QueueMissingException;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OptionsProviderImpl implements OptionsProvider {

    private final QueueManager manager;

    @Inject
    public OptionsProviderImpl(QueueManager manager) {
        this.manager = manager;
    }

    @Override
    @Transactional
    public void setConcurrency(String queueName, Long amount) {
        manager.setMaximumConcurrency(queueName, amount);
    }

    @Override
    public LongResponse getConcurrency(String queueName) {
        Long concurrency = manager.getMaximumConcurrency(queueName);

        if (concurrency == null) {
            throw new QueueMissingException("Queue with name " + queueName + " not found.", queueName);
        }

        return LongResponse
                .builder()
                .number(concurrency)
                .build();
    }
}
