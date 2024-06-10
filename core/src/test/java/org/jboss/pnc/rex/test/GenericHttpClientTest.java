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

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.TestData.createMockTask;
import static org.jboss.pnc.rex.test.common.TestData.getRequestFromSingleTask;
import static io.restassured.RestAssured.given;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class GenericHttpClientTest extends AbstractTest {
    @Inject
    HttpEndpoint endpoint;

    @Inject
    TaskEndpoint taskEndpoint;

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskEndpointURI;

    @Test
    void shouldRetryBackpressureOn425AndSucceed() {
        int amountOf425UntilSuccessful = 5;
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask(
                "backoff-test",
                Mode.ACTIVE,
                TestData.getRequestWithBackoff(null, amountOf425UntilSuccessful),
                TestData.getStopRequest(null),
                null));

        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "backoff-test");

        assertThat(endpoint.getCount()).isEqualTo(amountOf425UntilSuccessful);
    }

    @Test
    void shouldRetryOn425AndFailToStart() {
        int amountOf425UntilSuccessful = Integer.MAX_VALUE; // from application.yaml expiry for fallback is set to 5sec
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask(
                "backoff-test",
                Mode.ACTIVE,
                TestData.getRequestWithBackoff(null, amountOf425UntilSuccessful),
                TestData.getStopRequest(null),
                null));

        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.START_FAILED, "backoff-test");

        assertThat(endpoint.getCount()).isNotZero();
    }

    @Test
    void shouldPassMDCInBodyLocalConfig() {
        // with
        ConfigurationDTO config = ConfigurationDTO.builder()
                .passMDCInRequestBody(true)
                .mdcHeaderKeyMapping(Map.of("mdc-key", "mdc-logging-key"))
                .build();
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask("mdc-test",
                Mode.ACTIVE,
                TestData.getRequestWithStart(null, List.of(new Request.Header("mdc-key", "mdc-value"))),
                TestData.getStopRequest(null),
                null,
                config));

        // when
        endpoint.startRecordingQueue();
        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "mdc-test");
        endpoint.stopRecording();

        // then
        Collection<Object> recordedRequestData = endpoint.getRecordedRequestData();
        assertThat(recordedRequestData.toArray()[0])
                .isInstanceOf(StartRequest.class)
                .extracting("mdc")
                .isInstanceOf(Map.class)
                .satisfies((mdcMap) -> assertThat((Map<String, String>) mdcMap).containsEntry("mdc-logging-key", "mdc-value"));
    }

    @Test
    void shouldPassMDCInBodyGraphConfig() {
        // with
        ConfigurationDTO config = ConfigurationDTO.builder()
                .passMDCInRequestBody(true)
                .mdcHeaderKeyMapping(Map.of("mdc-key", "mdc-logging-key"))
                .build();
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask("mdc-test-graph",
                    Mode.ACTIVE,
                    TestData.getRequestWithStart(null, List.of(new Request.Header("mdc-key", "mdc-value"))),
                    TestData.getStopRequest(null),
                    null))
                .toBuilder()
                .graphConfiguration(config)
                .build();

        // when
        endpoint.startRecordingQueue();
        // using restassured because of weird MDC issue in main
        given().body(request)
                .contentType(ContentType.JSON)
                .when().post(taskEndpointURI).then()
                .statusCode(200);

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "mdc-test-graph");
        endpoint.stopRecording();

        // then
        Collection<Object> recordedRequestData = endpoint.getRecordedRequestData();
        assertThat(recordedRequestData.toArray()[0])
                .isInstanceOf(StartRequest.class)
                .extracting("mdc")
                .isInstanceOf(Map.class)
                .satisfies((mdcMap) -> assertThat((Map<String, String>) mdcMap).containsEntry("mdc-logging-key", "mdc-value"));
    }

    @Test
    void shouldPassMDCInBodyLocalTakePriority() {
        // with
        ConfigurationDTO graphConfig = ConfigurationDTO.builder()
                .passMDCInRequestBody(true)
                .mdcHeaderKeyMapping(Map.of("mdc1-key", "mdc1-logging-key"))
                .build();

        ConfigurationDTO localConfig = ConfigurationDTO.builder()
                .passMDCInRequestBody(true)
                .mdcHeaderKeyMapping(Map.of("mdc-key", "mdc-logging-key"))
                .build();
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask("mdc-test-graph",
                    Mode.ACTIVE,
                    TestData.getRequestWithStart(null,
                            List.of(new Request.Header("mdc-key", "mdc-value"),
                                    new Request.Header("mdc1-key", "mdc1-value"))),
                    TestData.getStopRequest(null),
                    null,
                    localConfig))
                .toBuilder()
                .graphConfiguration(graphConfig)
                .build();

        // when
        endpoint.startRecordingQueue();

        // using restassured because of weird MDC issue in main
        given().body(request)
                .contentType(ContentType.JSON)
                .header(new Header("mdc1-key", "mdc1-value"))
                .when()
                .post(taskEndpointURI)
                .then()
                .statusCode(200);

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "mdc-test-graph");
        endpoint.stopRecording();

        // then
        Collection<Object> recordedRequestData = endpoint.getRecordedRequestData();
        assertThat(recordedRequestData.toArray()[0])
                .isInstanceOf(StartRequest.class)
                .extracting("mdc")
                .isInstanceOf(Map.class)
                .satisfies((mdcMap) -> assertThat((Map<String, String>) mdcMap)
                        // from local-level config
                        .containsEntry("mdc-logging-key", "mdc-value")
                        // from graph-level config
                        .doesNotContainEntry("mdc1-logging-key", "mdc1-value"));
    }

    @Test
    void shouldPassMDCInBodyAndMergeConfigs() {
        // with
        ConfigurationDTO graphConfig = ConfigurationDTO.builder()
                .passMDCInRequestBody(true)
                .build();

        ConfigurationDTO localConfig = ConfigurationDTO.builder()
                .mdcHeaderKeyMapping(Map.of("mdc-key", "mdc-logging-key"))
                .build();
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask("mdc-test-graph",
                    Mode.ACTIVE,
                    TestData.getRequestWithStart(null,
                            List.of(new Request.Header("mdc-key", "mdc-value"),
                                    new Request.Header("mdc1-key", "mdc1-value"))),
                    TestData.getStopRequest(null),
                    null,
                    localConfig))
                .toBuilder()
                .graphConfiguration(graphConfig)
                .build();

        // when
        endpoint.startRecordingQueue();

        // using restassured because of weird MDC issue in main
        given().body(request)
                .contentType(ContentType.JSON)
                .when()
                .post(taskEndpointURI)
                .then()
                .statusCode(200);

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "mdc-test-graph");
        endpoint.stopRecording();

        // then
        Collection<Object> recordedRequestData = endpoint.getRecordedRequestData();
        assertThat(recordedRequestData.toArray()[0])
                .isInstanceOf(StartRequest.class)
                .extracting("mdc")
                .isInstanceOf(Map.class)
                .satisfies((mdcMap) -> assertThat((Map<String, String>) mdcMap).containsEntry("mdc-logging-key", "mdc-value"));
    }
}
