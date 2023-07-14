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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.model.Header;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import static java.time.Duration.of;

@Slf4j
@ApplicationScoped
public class GenericVertxHttpClient {

    private final WebClient client;
    private final List<Throwable> failFast;

    public GenericVertxHttpClient(
            Vertx vertx,
            @ConfigProperty(name = "scheduler.options.retry-policy.abortOn") List<Throwable> abortOn) {
        this.client = WebClient.create(vertx);
        this.failFast = abortOn;
    }

    /**
     * Must be ran on a thread that can block
     *
     * @param remoteEndpoint
     * @param method
     * @param headers
     * @param requestBody
     * @param onResponse
     */
    public void makeRequest(
            URI remoteEndpoint,
            Method method,
            List<Header> headers,
            Object requestBody,
            Consumer<HttpResponse<Buffer>> onResponse,
            Consumer<Throwable> onConnectionUnreachable) {
        HttpRequest<Buffer> request = client.request(
                toVertxMethod(method),
                getPort(remoteEndpoint),
                remoteEndpoint.getHost(),
                remoteEndpoint.getPath());
        addHeaders(request, headers);
        request.ssl(isSSL(remoteEndpoint));
        request.followRedirects(true);

        log.trace(
                "HTTP-CLIENT : Making request \n URL: {}\n METHOD: {}\n HEADERS: {}\n BODY: {}",
                remoteEndpoint,
                method,
                headers.toString(),
                requestBody.toString());

        var uni = Uni.createFrom().item(() -> request.sendJsonAndAwait(requestBody));

        handleRequest(uni, onResponse, onConnectionUnreachable).await().atMost(Duration.ofSeconds(5));
    }

    private boolean isSSL(URI remoteEndpoint) {
        return remoteEndpoint.getScheme().equals("https");
    }

    private static int getPort(URI remoteEndpoint) {
        if (remoteEndpoint.getPort() == -1) {
            if (remoteEndpoint.getScheme().equals("http")) {
                return 80;
            }

            if (remoteEndpoint.getScheme().equals("https")) {
                return 443;
            }

            return 80;
        }

        return remoteEndpoint.getPort();
    }

    private Uni<HttpResponse<Buffer>> handleRequest(
            Uni<HttpResponse<Buffer>> uni,
            Consumer<HttpResponse<Buffer>> onResponse,
            Consumer<Throwable> onConnectionUnreachable) {
        // case when http request succeeds and a body is received
        uni = uni.onItem()
                // create a separate uni to decouple internal failure tolerance
                .transformToUni(
                        i -> Uni.createFrom()
                                .item(i)
                                .invoke(onResponse)
                                .onFailure(th -> !failFast.contains(th))
                                .retry()
                                .withBackOff(of(10, ChronoUnit.MILLIS), of(100, ChronoUnit.MILLIS))
                                .atMost(20)
                                .onFailure()
                                // recover with null so that Uni doesn't trigger outer onFailure() handlers
                                .recoverWithNull());

        // cases when http request method itself fails (unreachable host)
        uni = uni.onFailure()
                .invoke(t -> log.warn("HTTP-CLIENT : Http call failed. RETRYING. Reason: ", t))
                .onFailure()
                .retry()
                .atMost(20)
                .onFailure()
                .invoke(onConnectionUnreachable)
                // recover with null so that Uni doesn't propagate the exception
                .onFailure()
                .recoverWithNull();

        return uni;
    }

    public Uni<HttpResponse<Buffer>> makeReactiveRequest(
            URI remoteEndpoint,
            Method method,
            List<Header> headers,
            Object requestBody,
            Consumer<HttpResponse<Buffer>> onResponse,
            Consumer<Throwable> onConnectionUnreachable) {
        HttpRequest<Buffer> request = client.request(
                toVertxMethod(method),
                getPort(remoteEndpoint),
                remoteEndpoint.getHost(),
                remoteEndpoint.getPath());
        addHeaders(request, headers);
        request.ssl(isSSL(remoteEndpoint));
        request.followRedirects(true);

        log.trace(
                "HTTP-CLIENT : Making request \n URL: {}\n METHOD: {}\n HEADERS: {}\n BODY: {}",
                remoteEndpoint,
                method,
                headers.toString(),
                requestBody.toString());

        return handleRequest(request.sendJson(requestBody), onResponse, onConnectionUnreachable);
    }

    private HttpMethod toVertxMethod(Method method) {
        return switch (method) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case PATCH -> HttpMethod.PATCH;
            case DELETE -> HttpMethod.DELETE;
            case HEAD -> HttpMethod.HEAD;
            case OPTIONS -> HttpMethod.OPTIONS;
        };
    }

    private void addHeaders(HttpRequest<Buffer> request, List<Header> headers) {
        if (headers != null) {
            headers.forEach(header -> request.putHeader(header.getName(), header.getValue()));
        }
    }
}
