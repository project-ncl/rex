package org.jboss.pnc.rex.core;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.rest.api.InternalEndpoint;
import org.jboss.pnc.rex.rest.api.TaskEndpoint;
import org.jboss.pnc.rex.test.infinispan.InfinispanResource;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.jboss.pnc.rex.core.common.TestData.getMockTaskWithoutStart;

@QuarkusTest
@QuarkusTestResource(InfinispanResource.class)
public class ValidationTest {

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskEndpointURI;

    @TestHTTPEndpoint(InternalEndpoint.class)
    @TestHTTPResource
    URI internalEndpointURI;

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
                    .post(internalEndpointURI.getPath() + "/1/finish")
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
                    .post(internalEndpointURI.getPath() + "/1/finish")
                .then()
                    .statusCode(400)
                    .body("errorType", containsString("ViolationException"));
    }

}
