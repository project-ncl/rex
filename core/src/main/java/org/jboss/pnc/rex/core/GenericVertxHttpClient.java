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


import io.quarkus.oidc.client.OidcClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.impl.ClientPhase;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.control.ActivateRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.exceptions.HttpResponseException;
import org.jboss.pnc.rex.common.exceptions.RequestRetryException;
import org.jboss.pnc.rex.core.config.RequestRetryPolicy;
import org.jboss.pnc.rex.core.config.InternalRetryPolicy;
import org.jboss.pnc.rex.core.config.api.HttpConfiguration;
import org.jboss.pnc.rex.model.Header;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import static java.time.Duration.of;

@Slf4j
@ApplicationScoped
public class GenericVertxHttpClient {

    private final WebClient client;
    private final InternalRetryPolicy internalPolicy;
    private final HttpConfiguration configuration;
    private final RequestRetryPolicy requestRetryPolicy;
    private final StatusCodeRetryPolicy statusCodeRetryPolicy;
    private final OidcClient oidcClient;

    public GenericVertxHttpClient(Vertx vertx,
                                  InternalRetryPolicy internalPolicy,
                                  HttpConfiguration configuration,
                                  OidcClient oidcClient) {
        this.client = WebClient.create(vertx);
        this.internalPolicy = internalPolicy;
        this.configuration = configuration;
        this.requestRetryPolicy = configuration.requestRetryPolicy();
        this.statusCodeRetryPolicy = configuration.statusCodeRetryPolicy();
        this.oidcClient = oidcClient;
        WebClientInternal delegate = (WebClientInternal) client.getDelegate();
        delegate.addInterceptor(this::putOrRefreshToken);
    }

    /**
     * Must be ran on a thread that can block
     * @param remoteEndpoint
     * @param method
     * @param headers
     * @param requestBody
     * @param onResponse
     */
    @ActivateRequestContext
    public void makeRequest(URI remoteEndpoint,
                             Method method,
                             List<Header> headers,
                             Object requestBody,
                             Consumer<HttpResponse<Buffer>> onResponse,
                             Consumer<Throwable> onConnectionUnreachable) {
        makeReactiveRequest(remoteEndpoint,
                method,
                headers,
                requestBody,
                onResponse,
                onConnectionUnreachable)
            .await() // TODO make configurable?
            .indefinitely();
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

    private Uni<HttpResponse<Buffer>> handleRequest(Uni<HttpResponse<Buffer>> uni, Consumer<HttpResponse<Buffer>> onResponse, Consumer<Throwable> onConnectionUnreachable) {
        // apply retry if http response error is received
        uni = uni.onItem().invoke(Unchecked.consumer(resp -> {
                    if (resp != null && statusCodeRetryPolicy.shouldRetry(resp.statusCode())) {
                        throw new HttpResponseException(resp.statusCode());
                    }
                }));
        // cases when http response is eligible for a retry
        uni = statusCodeRetryPolicy.applyRetryPolicy(uni);

        // case when http request succeeds and a body is received
        uni = uni.onItem()
                // create a separate uni to decouple internal failure tolerance
                .transformToUni(i -> Uni.createFrom()
                    .item(i)
                    .invoke(onResponse)
                    .onFailure(this::abortOnNonRecoverable)
                        .retry()
                            .withBackOff(of(10, ChronoUnit.MILLIS), of(100, ChronoUnit.MILLIS))
                            .withJitter(0.5)
                            .atMost(20)
                    .onFailure(throwable -> !(throwable instanceof RequestRetryException)) // propagate to exception to outer loop
                        // recover with null so that Uni doesn't trigger outer onFailure() handlers
                        .recoverWithNull()
                );

        // cases when http request method itself fails (unreachable host)
        uni = uni.onFailure(this::skipOnResponse)
                .invoke(t ->  {
                    if (t instanceof RequestRetryException) {
                        log.warn("HTTP-CLIENT : Http call failed. RETRYING.");
                    } else if (t instanceof HttpResponseException hre) {
                        log.warn("HTTP-CLIENT : Http call failed ({}). RETRYING.", hre.getStatusCode());
                    }
                });
        uni = requestRetryPolicy.applyToleranceOn(this::skipOnResponse, uni);

        // fallback to connection unreachable after an exception
        Consumer<Throwable> onErrorResponse = t -> {
            if (!(t instanceof HttpResponseException) && t.getCause() instanceof HttpResponseException) {
                // prevent java.lang.IllegalStateException in case of exhausted retries due to expire-in limit
                onConnectionUnreachable.accept(t.getCause());
            } else {
                onConnectionUnreachable.accept(t);
            }
        };
        uni = uni.onFailure().invoke(onErrorResponse)
                // recover with null so that Uni doesn't propagate the exception
                .onFailure().recoverWithNull();

        return uni;
    }

    private boolean abortOnNonRecoverable(Throwable failure) {
        return !internalPolicy.abortOn().contains(failure.getClass())
            // This is internal retry loop, RequestRetryException to go into outer Retry loop where the request is retried
            && !(failure instanceof RequestRetryException);
    }

    /**
     * @return false when exception is not caused by http response with an error code.
     */
    private boolean skipOnResponse(Throwable failure) {
        return !(failure instanceof HttpResponseException || failure.getCause() instanceof HttpResponseException);
    }

    @ActivateRequestContext
    public Uni<HttpResponse<Buffer>> makeReactiveRequest(URI remoteEndpoint,
                             Method method,
                             List<Header> headers,
                             Object requestBody,
                             Consumer<HttpResponse<Buffer>> onResponse,
                             Consumer<Throwable> onConnectionUnreachable) {
        HttpRequest<Buffer> request = client.request(toVertxMethod(method),
                getPort(remoteEndpoint),
                remoteEndpoint.getHost(),
                getPath(remoteEndpoint));
        addHeaders(request, headers);
        request.ssl(isSSL(remoteEndpoint));
        request.followRedirects(configuration.followRedirects());
        request.timeout(configuration.idleTimeout().toMillis());

        log.trace("HTTP-CLIENT : Making request \n URL: {}\n METHOD: {}\n HEADERS: {}\n BODY: {}",
                remoteEndpoint,
                method,
                headers.toString(),
                requestBody.toString());

        return handleRequest(
                request.sendJson(requestBody).emitOn(Infrastructure.getDefaultWorkerPool()),
                onResponse,
                onConnectionUnreachable);
    }

    private static String getPath(URI remoteEndpoint) {
        if (remoteEndpoint.getQuery() != null) {
            return remoteEndpoint.getPath() + "?" + remoteEndpoint.getQuery();
        }

        return remoteEndpoint.getPath();
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

    /**
     * Interceptor that is called every request. Ensures that the accessToken is refreshed during retries.
     *
     * @link <a href="https://stackoverflow.com/questions/76985918/custom-interceptor-in-quarkus-mutiny-web-client">Stack Overflow</a>
     * @param context httpContext
     */
    private void putOrRefreshToken(HttpContext<?> context) {
        try {
            if (context.phase() == ClientPhase.PREPARE_REQUEST) {
                io.vertx.ext.web.client.HttpRequest<?> request = context.request();

                String accessToken = oidcClient.getTokens().await().indefinitely().getAccessToken();

                request.bearerTokenAuthentication(accessToken);
            }
        } finally {
            // go to next interceptor
            context.next();
        }
    }
}
