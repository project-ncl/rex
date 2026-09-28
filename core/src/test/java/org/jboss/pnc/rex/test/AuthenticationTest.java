/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URI;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.test.profile.WithWiremockOpenId;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(WithWiremockOpenId.class)
public class AuthenticationTest {

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskEndpointURI;

    @TestHTTPEndpoint(QueueEndpoint.class)
    @TestHTTPResource
    URI queueEndpointURI;

    @Test
    void testWithoutAuthentication() {
        given()
                .when()
                .contentType(ContentType.JSON)
                .put(taskEndpointURI.getPath()+"/missing/cancel")
                .then()
                .statusCode(401);
    }

    @Test
    void testWithUserAuthentication() {
        given()
                .auth().oauth2(getAccessToken("alice", Set.of("pnc-app-rex-user")))
                .when()
                .contentType(ContentType.JSON)
                .put(taskEndpointURI.getPath()+"/missing/cancel")
                .then()
                .statusCode(400)
                .body("errorType", containsString("TaskMissingException"));
    }

    @Test
    void testWithAdminAuthentication() {
        given()
                .auth().oauth2(getAccessToken("admin", Set.of("pnc-app-rex-editor")))
                .when()
                .contentType(ContentType.JSON)
                .post(queueEndpointURI.getPath()+"/concurrency?amount=40")
                .then()
                .statusCode(204);
    }

    @Test
    void testWithUserOnAdminAuthentication() {
        given()
                .auth().oauth2(getAccessToken("jdoe", Set.of("user")))
                .when()
                .contentType(ContentType.JSON)
                .post(queueEndpointURI.getPath()+"/concurrency?amount=40")
                .then()
                .statusCode(403);
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();
    }
}
