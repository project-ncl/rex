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
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.ErrorOption;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.jboss.pnc.rex.test.common.TestData.getMockTaskWithoutStart;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class ValidationTest {

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskEndpointURI;

    @TestHTTPEndpoint(CallbackEndpoint.class)
    @TestHTTPResource
    URI callbackEndpointURI;

    @Test
    void testCreateNoBody() {
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testCreateWithBlankEdge() {
        CreateGraphRequest body = CreateGraphRequest.builder().edge(EdgeDTO.builder().source("hello").target("").build()).build();
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testCreateWithBlankVertex() {
        CreateGraphRequest body = CreateGraphRequest.builder()
                .vertex("", getMockTaskWithoutStart("", Mode.IDLE))
                .build();
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testCreateWithNullTask() {
        CreateGraphRequest body = CreateGraphRequest.builder()
                .vertex("", null)
                .build();
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testCreateWithNullEndpoints() {
        CreateGraphRequest body = CreateGraphRequest.builder()
                .vertex("task", getMockTaskWithoutStart("task", Mode.IDLE)
                        .toBuilder()
                        .remoteStart(null)
                        .build())
                .build();

        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));

        body = CreateGraphRequest.builder()
                .vertex("task", getMockTaskWithoutStart("task", Mode.IDLE)
                        .toBuilder()
                        .remoteCancel(null)
                        .build())
                .build();

        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(taskEndpointURI.getPath())
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testFinishWithNoBody() {
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .post(callbackEndpointURI.getPath() + CallbackEndpoint.FINISH_TASK_FMT.formatted( "1"))
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void testFinishWithNoStatus() {
        FinishRequest request = new FinishRequest(null,"HELLO");
        given()
                .when()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post(callbackEndpointURI.getPath() + CallbackEndpoint.FINISH_TASK_FMT.formatted( "1"))
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

    @Test
    void shouldNotFailOnMissingTaskIfIgnoreSpecified() {
        FinishRequest request = new FinishRequest(true,"HELLO");
        given()
            .when()
                .contentType(ContentType.JSON)
                .body(request)
                .queryParam("err", ErrorOption.IGNORE) // IGNORE should make the response 204
                .post(callbackEndpointURI.getPath() + CallbackEndpoint.FINISH_TASK_FMT.formatted( "doesn't-exist"))
            .then()
                .statusCode(204);
    }
}
