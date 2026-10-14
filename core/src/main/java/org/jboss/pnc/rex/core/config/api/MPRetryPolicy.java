/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.config.api;

import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import io.smallrye.config.WithDefault;
import io.smallrye.faulttolerance.api.Guard;

public interface MPRetryPolicy {

    /**
     * Fixed delay between retries in millis. The jitter is applied on each retry.
     *
     * @return delay between retries in millis
     */
    @PositiveOrZero
    @WithDefault("0")
    long delay();

    /**
     * Jitter of duration in millis that is applied on each retry. The result will give a delay in range of
     * [delay - jitter, delay + jitter]
     *
     * The jitter can't cause the retry delay to fall below 0.
     *
     * @return jitter on each retry in duration millis
     */
    @PositiveOrZero
    @WithDefault("0")
    long jitter();

    /**
     * The maximum amount of retries until a threshold is met and failure is propagated.
     *
     * Value of -1 means infinite retries whilst 0 disables retry policy.
     *
     * @return threshold of maximum retries
     */
    @Min(-1)
    @WithDefault("3")
    int maxRetries();

    /**
     * List of Exceptions which are deemed non-recoverable. In case the method in context throws any of these
     * exceptions,
     * the method is NOT retried and the failure is propagated immediately.
     *
     * @return list of non-recoverable exceptions
     */
    List<Class<? extends Throwable>> abortOn();

    default <T> Guard.Builder toleranceBuilder(Class<T> clazz, Runnable onRetry, String description) {
        return Guard.<T> create()
                .withDescription(description)
                .withRetry()
                .maxRetries(maxRetries())
                .abortOn(abortOn())
                .delay(delay(), ChronoUnit.MILLIS)
                .jitter(jitter(), ChronoUnit.MILLIS)
                .onRetry(onRetry)
                .done();
    }
}
