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
package org.jboss.pnc.rex.core.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.jboss.pnc.rex.common.exceptions.HttpResponseException;
import org.jboss.pnc.rex.core.config.api.MutinyRetryPolicy;

import java.util.Map;

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
 */
@ConfigMapping
public interface StatusCodeRetryPolicy {

    @WithParentName
    Map<String, MutinyRetryPolicy> statusCodeRetryPolicy();

    default boolean shouldRetry(int code) {
        return Matcher.isExactMatchPresent(statusCodeRetryPolicy(), code)
          || statusCodeRetryPolicy().keySet().contains(Matcher.toWildcard(code));
    }

    default Uni<HttpResponse<Buffer>> applyRetryPolicy(Uni<HttpResponse<Buffer>> uni) {
        Uni<HttpResponse<Buffer>> newUni = uni;
        for (var entry : statusCodeRetryPolicy().entrySet()) {
            newUni = entry.getValue().applyToleranceOn(
                (e) -> {
                    if (e instanceof HttpResponseException hre) {
                        // if there is exact matching policy present, return true only if this entry is it
                        if (Matcher.isExactMatchPresent(statusCodeRetryPolicy(), hre.getStatusCode())) {
                            return entry.getKey().equals(Integer.toString(hre.getStatusCode()));
                        } else {
                            // if there is no exact match present return true if this entry is a wildcard match
                            return entry.getKey().equals(Matcher.toWildcard(hre.getStatusCode()));
                        }
                    } else {
                        return false;
                    }
                },
                newUni);
        }
        return newUni;
    }

    class Matcher {

        private static String toWildcard(int code) {
            return (code / 100) + "xx";
        }

        private static boolean isExactMatchPresent(Map<String, MutinyRetryPolicy> statusCodeRetryPolicy, int code) {
            return statusCodeRetryPolicy
                .keySet()
                .contains(Integer.toString(code));
        }
    }
}
