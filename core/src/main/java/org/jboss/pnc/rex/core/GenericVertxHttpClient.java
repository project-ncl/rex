/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.model.Header;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@ApplicationScoped
public class GenericVertxHttpClient {

    private final WebClient client;

    public GenericVertxHttpClient(Vertx vertx) {
        this.client = WebClient.create(vertx);
    }

    /**
     * Must be ran on a thread that can block
     * @param remoteEndpoint
     * @param method
     * @param headers
     * @param requestBody
     * @param onResponse
     */
    public void makeRequest(URI remoteEndpoint,
                             Method method,
                             List<Header> headers,
                             Object requestBody,
                             Consumer<HttpResponse<Buffer>> onResponse,
                             Consumer<Throwable> onConnectionUnreachable) {
        HttpRequest<Buffer> request = client.request(toVertxMethod(method),
                remoteEndpoint.getPort() == -1 ? 80 : remoteEndpoint.getPort(),
                remoteEndpoint.getHost(),
                remoteEndpoint.getPath());
        addHeaders(request, headers);

        log.trace("HTTP-CLIENT : Making request \n URL: {}\n METHOD: {}\n HEADERS: {}\n BODY: {}",
                remoteEndpoint,
                method,
                headers.toString(),
                requestBody.toString());

        Uni.createFrom().item(() -> request.sendJsonAndAwait(requestBody))
            .onItem().transformToUni(i -> Uni.createFrom()
                .item(i)
                .onItem().invoke(onResponse)
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull())
            .onFailure().invoke(t -> log.warn("HTTP-CLIENT : Http call failed. RETRYING. Reason: {}", t.toString()))
            .onFailure().retry().atMost(5)
            .onFailure().invoke(onConnectionUnreachable)
            // recover with null so that Uni doesn't propagate the exception
            .onFailure().recoverWithNull()
            .await().indefinitely();
    }
    private <T> T wrapExceptions(Supplier<T> supplier) {
        try {
           return supplier.get();
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private HttpMethod toVertxMethod(Method method) {
        switch (method) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case PATCH:
                return HttpMethod.PATCH;
            case DELETE:
                return HttpMethod.DELETE;
            case HEAD:
                return HttpMethod.HEAD;
            case OPTIONS:
                return HttpMethod.OPTIONS;
        }
        return null;
    }

    private void addHeaders(HttpRequest<Buffer> request, List<Header> headers) {
        if (headers != null) {
            headers.forEach(header -> request.putHeader(header.getName(), header.getValue()));
        }
    }
}
