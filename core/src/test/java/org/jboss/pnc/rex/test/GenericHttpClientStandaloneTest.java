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
package org.jboss.pnc.rex.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.exceptions.HttpResponseException;
import org.jboss.pnc.rex.core.GenericVertxHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@QuarkusTest
public class GenericHttpClientStandaloneTest {

    private static WireMockServer wireMockServer;
    private static final int MOCK_SERVER_PORT = 8083;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(MOCK_SERVER_PORT));
        wireMockServer.start();
        WireMock.configureFor("localhost", MOCK_SERVER_PORT);
    }

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Inject
    GenericVertxHttpClient httpClient;

    @ParameterizedTest
    @ValueSource(ints = {425, 429, 500, 503, 599})
    void shouldRetryOnErrorCodeAndSucceed(int code) throws InterruptedException {
        // given
        // 1st request: respond with error
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(code))
                    .inScenario("retry-" + code)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("attempt1"));
        // 2nd request: respond with error
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(code))
                    .inScenario("retry-" + code)
                    .whenScenarioStateIs("attempt1")
                    .willSetStateTo("attempt2"));
        // 3rd request: respond with success
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(200))
                    .inScenario("retry-" + code)
                    .whenScenarioStateIs("attempt2")
                    .willSetStateTo("attempt3"));

        ArrayBlockingQueue<HttpResponse<Buffer>> responses = new ArrayBlockingQueue<>(10);
        Consumer<HttpResponse<Buffer>> onResponse = r -> {
            responses.add(r);
        };

        // when
        httpClient.makeReactiveRequest(
            URI.create("http://localhost:" + MOCK_SERVER_PORT),
            Method.GET,
            Collections.emptyList(),
            "",
            onResponse,
            (r) -> {})
            .await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // expect
        HttpResponse<Buffer> response = responses.poll(5, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(ints = {425, 429, 500, 503, 599})
    void shouldFailIfNoSuccessCodeReceived(int code) throws InterruptedException {
        // given
        // always respond with error
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(code).withBody("Error response body."))
        );

        ArrayBlockingQueue<Throwable> failures = new ArrayBlockingQueue<>(10);
        Consumer<Throwable> onFailure = throwable -> {
            failures.add(throwable);
        };

        // when
        httpClient.makeReactiveRequest(
                      URI.create("http://localhost:" + MOCK_SERVER_PORT),
                      Method.GET,
                      Collections.emptyList(),
                      "",
                      (r) -> {},
                      onFailure)
                  .await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // expect
        Throwable throwable = failures.poll(5, TimeUnit.SECONDS);
        if (throwable instanceof HttpResponseException) {
            HttpResponseException e = (HttpResponseException) throwable;
            assertThat(e.getStatusCode()).isEqualTo(code);
        } else {
            Assertions.fail("Unexpected exception type.", throwable);
        }
    }

    @Test
    void shouldRetryOnFaultAndSucceed() throws InterruptedException {
        // given
        // 1st request: respond with error
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .inScenario("retry-fault")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("attempt1"));
        // 2nd request: respond with error
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .inScenario("retry-fault")
                    .whenScenarioStateIs("attempt1")
                    .willSetStateTo("attempt2"));
        // 3rd request: respond with success
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(200))
                    .inScenario("retry-fault")
                    .whenScenarioStateIs("attempt2")
                    .willSetStateTo("attempt3"));

        ArrayBlockingQueue<HttpResponse<Buffer>> responses = new ArrayBlockingQueue<>(10);
        Consumer<HttpResponse<Buffer>> onResponse = r -> {
            responses.add(r);
        };

        // when
        httpClient.makeReactiveRequest(
                      URI.create("http://localhost:" + MOCK_SERVER_PORT),
                      Method.GET,
                      Collections.emptyList(),
                      "",
                      onResponse,
                      (r) -> {})
                  .await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // expect
        HttpResponse<Buffer> response = responses.poll(5, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldNotRetryOn404() throws InterruptedException {
        // given
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(404))
        );

        // when
        httpClient.makeReactiveRequest(
                      URI.create("http://localhost:" + MOCK_SERVER_PORT + "/not-found"),
                      Method.GET,
                      Collections.emptyList(),
                      "",
                      (r) -> {},
                      (r) -> {}
                  )
                  .await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // expect
        verify(1, getRequestedFor(urlEqualTo("/not-found")));
    }

    @Test
    void shouldExpireOn425() throws InterruptedException {
        // given
        stubFor(get(urlPathMatching("/.*"))
                    .willReturn(aResponse().withStatus(425))
        );

        // when
        httpClient.makeReactiveRequest(
                      URI.create("http://localhost:" + MOCK_SERVER_PORT + "/back-off"),
                      Method.GET,
                      Collections.emptyList(),
                      "",
                      (r) -> {},
                      (r) -> {}
                  )
                  .await().atMost(Duration.of(10, ChronoUnit.SECONDS));

        // expect
        List<ServeEvent> allServeEvents = getAllServeEvents();
        log.info("Number of requests {}", allServeEvents.size());

        Assertions.assertTrue(allServeEvents.size() >= 2, "There should be at least 2 requests.");

        ServeEvent lastRequest = allServeEvents.get(0);
        ServeEvent firstRequest = allServeEvents.get(allServeEvents.size() - 1);
        log.info("First request {}, last request: {}", firstRequest.getRequest().getLoggedDate(), lastRequest.getRequest().getLoggedDate());
        Duration duration = Duration.between(firstRequest.getRequest().getLoggedDate().toInstant(), lastRequest.getRequest().getLoggedDate().toInstant());
        Assertions.assertTrue(duration.abs().toMillis() < 5000, "Should not retry for more than 5 sec.");
    }
}
