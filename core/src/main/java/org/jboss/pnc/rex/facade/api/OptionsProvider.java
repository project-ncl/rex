/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.api;

import jakarta.annotation.Nullable;

import org.jboss.pnc.rex.dto.responses.LongResponse;

/**
 * Public interface for managing scheduler's settings on runtime.
 */
public interface OptionsProvider {

    /**
     * Sets the maximum amount of concurrent Tasks. The method does not have effect on already running Tasks.
     *
     * @param amount amount to be set
     */
    void setConcurrency(@Nullable String queueName, Long amount);

    /**
     * Return current amount of concurrent tasks.
     *
     * @return maximum concurrent Tasks
     */
    LongResponse getConcurrency(@Nullable String queueName);
}
