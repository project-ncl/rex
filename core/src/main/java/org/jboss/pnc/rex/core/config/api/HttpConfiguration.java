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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.jboss.pnc.rex.core.config.HttpRetryPolicy;

import java.time.Duration;
import java.util.Map;

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
    HttpRetryPolicy requestRetryPolicy();

    /**
     * Configuration of http errors fault tolerance, where key is error code and value a FT policy.
     *
     * Supported Keys:
     *   '3xx', '4xx', '5xx': FT is applied to whole 'Nxx' group (string literal 'xx' must be used)
     *   NNN: FT is applied to exact error code NNN (eg. 500, 503)
     *
     * When both keys match the error code, more specific one is applied.
     * For example:
     *   5xx max-retries is set to 3 and
     *   500 max-retries is set to 5
     * All 5xx responses will be retried max 3 times except 500 will be retried max 5 times.
     * The configuration merge is done on a error group level (3xx, 4xx, 5xx), not on the individual fields defined in a `HttpRetryPolicy` object,
     * meaning whole `HttpRetryPolicy` defined for NNN can override the Nxx `HttpRetryPolicy`
     *
     * @return map of http error codes and related FT configuration
     */
    Map<String, HttpRetryPolicy> httpErrorRetryPolicy();

}
