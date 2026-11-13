/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.config.api;

import java.time.Duration;

import org.jboss.pnc.rex.core.config.RequestRetryPolicy;
import org.jboss.pnc.rex.core.config.StatusCodeRetryPolicy;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for internal HTTP client requests from Rex.
 */
@ConfigMapping(prefix = "scheduler.options.http-configuration") //CDI
public interface HttpConfiguration {

    /**
     * Timeout in millis for HTTP client in which the request times out.
     *
     * Value of 0 means NO timeout.
     *
     * @return duration until request times out
     */
    @WithDefault("5m") // 5 minutes
    Duration idleTimeout();

    /**
     * Configures HTTP client to follow 3xx redirects.
     *
     * @return boolean
     */
    @WithDefault("true")
    boolean followRedirects();

    /**
     * Configuration of fault tolerance in case of Unreachable Host, Timeouts, DNS, TLS....
     *
     * If the fault tolerance does not result in a successful request, usually a fallback is triggered.
     *
     * @return FT configuration for unexpected failures
     */
    RequestRetryPolicy requestRetryPolicy();

    /**
     * Configuration of fault tolerance in case when http error response is received.
     */
    StatusCodeRetryPolicy statusCodeRetryPolicy();

}
