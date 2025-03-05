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
import io.smallrye.config.WithName;
import io.smallrye.mutiny.Uni;

import org.jboss.pnc.rex.core.config.validation.ValidBackoff;

import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;

public interface MutinyRetryPolicy {

    /**
     * Configures a fixed maximum amount of retries until the fault tolerance stops.
     *
     * Value of 0 means retries are disabled.
     *
     * Either {@link #maxRetries()} or {@link #expireIn()} must be configured.
     *
     * @return amount of retries until a failure occurs
     */
    @WithDefault("0")
    int maxRetries();

    /**
     * Configures a maximum amount of TIME in MILLIS during which Rex will try to retry. The {@link #backoff()} policy
     * is applied on each retry.
     *
     * Value of 0 means expiry is disabled.
     *
     * Either {@link #maxRetries()} or {@link #expireIn()} must be configured. If both are configured
     * {@link #maxRetries()} takes precedence.
     *
     * @return time in millis until a failure occurs
     */
    @WithDefault("0")
    Duration expireIn();

    ExpBackoff backoff();

    /**
     * Exponential backoff configuration.
     */
    @ValidBackoff
    interface ExpBackoff {

        /**
         * Both Initial and Minimum delay between retries. Jitter factor can't cause the delay to fall below this value.
         *
         * @return minimum delay in millis
         */
        @WithDefault("0")
//        @DurationMin(nanos = 1) // positive
        @WithName("min-delay")
        Duration initialDelay();

        /**
         * Maximum delay between retries. Jitter factor can't cause the delay to get above this value.
         *
         * @return maximum delay in millis
         */
        @WithDefault("0")
        Duration maxDelay();

        /**
         * Jitter factor which is applied on each retry. Jitter factor takes the fixed exponential delay and applies
         * jitter as a factor. Therefore, the delay will be in range of
         *    [delay - (delay * jitterFactor), delay + (delay * jitterFactor)]
         *
         * @return  millis
         */
        @WithDefault("0.5")
        Double jitterFactor();
    }

    default <T> Uni<T> applyToleranceOn(Predicate<? super Throwable> predicate, Uni<T> uni) {
        if (maxRetries() != 0 || !expireIn().isZero()) {
            var retry = uni.onFailure(predicate).retry();

            if (backoff().maxDelay().isZero() && !backoff().initialDelay().isZero()) {
                retry = retry.withBackOff(backoff().initialDelay()); // MD: 0 ID: non-0
            } else {
                retry = retry.withBackOff(backoff().initialDelay(), backoff().maxDelay()); // MD: non-0 ID: 0
            }

            if (backoff().jitterFactor() != 0) {
                retry = retry.withJitter(backoff().jitterFactor());
            }

            if (maxRetries() != 0) {
                return retry.atMost(maxRetries());
            }

            if (!expireIn().isZero()) {
                return retry.expireIn(expireIn().toMillis());
            }
        }

        // no tolerance applied
        return uni;
    }

    default <T> Uni<T> applyToleranceOn(Class<? extends Throwable> typeOfFailure, Uni<T> uni) {
        return applyToleranceOn(typeOfFailure::isInstance, uni);
    }

    /**
     * Apply tolerance only if none of the listed types matches the exception type.
     */
    default <T> Uni<T> applyToleranceExceptOn(Set<Class<? extends RuntimeException>> shouldNotMatch, Uni<T> uni) {
        return applyToleranceOn(t -> shouldNotMatch.stream().noneMatch(c -> c.isInstance(t)), uni);
    }


    default <T> Uni<T> applyTolerance(Uni<T> uni) {
        return applyToleranceOn(__ -> true, uni);
    }
}
