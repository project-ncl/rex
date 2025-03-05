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
package org.jboss.pnc.rex.core;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.jboss.pnc.rex.common.exceptions.HttpResponseException;
import org.jboss.pnc.rex.core.config.HttpRetryPolicy;

import java.util.HashMap;
import java.util.Map;

public class HttpErrorRetryPolicyConfiguration {
    private final Map<Integer, HttpRetryPolicy> errCodesRetryPolicy = new HashMap<>() {};

    public HttpErrorRetryPolicyConfiguration(Map<String, HttpRetryPolicy> requestRetryPolicy) {
        for (var code : requestRetryPolicy.keySet()) {
            if (code.equals("3xx")) {
                for (int i = 300; i <= 399; i++) {
                    this.errCodesRetryPolicy.put(i, requestRetryPolicy.get("3xx"));
                }
            } else if (code.equals("4xx")) {
                for (int i = 400; i <= 499; i++) {
                    this.errCodesRetryPolicy.put(i, requestRetryPolicy.get("4xx"));
                }
            } else if (code.equals("5xx")) {
                for (int i = 500; i <= 599; i++) {
                    this.errCodesRetryPolicy.put(i, requestRetryPolicy.get("5xx"));
                }
            } else if (code.matches("^\\d{3}$")) {
                errCodesRetryPolicy.put(Integer.valueOf(code), requestRetryPolicy.get(code));
            }
        }
    }

    public boolean shouldRetry(int code) {
        return errCodesRetryPolicy.containsKey(code);
    }

    public Uni<HttpResponse<Buffer>> applyRetryPolicy(Uni<HttpResponse<Buffer>> uni) {
        Uni<HttpResponse<Buffer>> newUni = uni;
        for (var entry : errCodesRetryPolicy.entrySet()) {
            newUni = entry.getValue().applyToleranceOn(
                (e) -> {
                    if (e instanceof HttpResponseException) {
                        return entry.getKey().equals(((HttpResponseException) e).getStatusCode());
                    } else {
                        return false;
                    }
                },
                newUni);
        }
        return newUni;
    }
}
