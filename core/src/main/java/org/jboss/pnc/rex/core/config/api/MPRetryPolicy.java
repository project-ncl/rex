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
package org.jboss.pnc.rex.core.config.api;

import io.smallrye.config.WithDefault;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

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
     *     [delay - jitter, delay + jitter]
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
     * List of Exceptions which are deemed non-recoverable. In case the method in context throws any of these exceptions,
     * the method is NOT retried and the failure is propagated immediately.
     *
     * @return list of non-recoverable exceptions
     */
    List<Throwable> abortOn();
}
