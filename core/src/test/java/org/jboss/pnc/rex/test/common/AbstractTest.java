/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.common;

import static io.restassured.RestAssured.*;

import java.net.URI;

import jakarta.inject.Inject;

import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.test.endpoints.TransitionRecorderEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
@TestSecurity(user = "admin", roles = { "pnc-users-admin" })
public abstract class AbstractTest {

    @TestHTTPEndpoint(QueueEndpoint.class)
    @TestHTTPResource
    URI queueEndpoint;

    @TestHTTPEndpoint(MaintenanceEndpoint.class)
    @TestHTTPResource
    URI maintenanceEndpoint;

    @Inject
    TransitionRecorder recorder;

    @Inject
    HttpEndpoint httpEndpoint;

    @Inject
    TransitionRecorderEndpoint recorderEndpoint;

    @BeforeEach
    public void resetEverything() {
        resetEverythingWithMax(1000L);
    }

    @AfterEach
    public void clearResources() throws InterruptedException {
        recorder.clear();
        recorderEndpoint.flush();
        httpEndpoint.clear();

        //Uncomment if you're encountering race conditions.
        // Thread.sleep(100);
    }

    public void resetEverythingWithMax(long max) {
        // clear caches
        given()
                .contentType(ContentType.JSON)
                .post(maintenanceEndpoint.getPath() + MaintenanceEndpoint.CLEAR_ALL)
                .then()
                .statusCode(204);

        // reset Maximum setting
        given()
                .queryParam("amount", max)
                .contentType(ContentType.JSON)
                .post(queueEndpoint.getPath() + QueueEndpoint.SET_CONCURRENT)
                .then()
                .statusCode(204);
        log.info("Clearing data completed.");
    }
}
