/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.pnc.rex.common.exceptions.QueueMissingException;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;

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
