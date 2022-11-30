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
package org.jboss.pnc.rex.core.authentication;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URI;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import org.jboss.pnc.rex.rest.api.InternalEndpoint;
import org.jboss.pnc.rex.rest.api.TaskEndpoint;
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

    @TestHTTPEndpoint(InternalEndpoint.class)
    @TestHTTPResource
    URI internalEndpointURI;

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
                .auth().oauth2(getAccessToken("alice", Set.of("user")))
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
                .auth().oauth2(getAccessToken("admin", Set.of("system-user")))
                .when()
                .contentType(ContentType.JSON)
                .post(internalEndpointURI.getPath()+"/options/concurrency?amount=40")
                .then()
                .statusCode(204);
    }

    @Test
    void testWithUserOnAdminAuthentication() {
        given()
                .auth().oauth2(getAccessToken("jdoe", Set.of("user")))
                .when()
                .contentType(ContentType.JSON)
                .post(internalEndpointURI.getPath()+"/options/concurrency?amount=40")
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
