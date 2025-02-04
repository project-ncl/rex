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
package org.jboss.pnc.rex.test.common;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.test.endpoints.TransitionRecorderEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;

import static io.restassured.RestAssured.*;

@Slf4j
@QuarkusTest
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
        httpEndpoint.clearRequestCounter();

        //Uncomment if you're encountering race conditions.
        Thread.sleep(100);
    }

    public void resetEverythingWithMax(long max) {
        // reset Maximum setting
        given()
            .queryParam("amount", max)
            .contentType(ContentType.JSON)
            .post(queueEndpoint.getPath() + QueueEndpoint.SET_CONCURRENT)
            .then()
            .statusCode(204);

        // clear caches
        given()
            .contentType(ContentType.JSON)
            .post(maintenanceEndpoint.getPath() + MaintenanceEndpoint.CLEAR_ALL)
            .then()
                .statusCode(204);

        log.info("Clearing data completed.");
    }
}
