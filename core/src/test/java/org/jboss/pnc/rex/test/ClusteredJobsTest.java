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

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.ClusteredJobRegistryImpl;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.core.jobs.TimeoutCancelClusterJob;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAre;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@Slf4j
public class ClusteredJobsTest extends AbstractTest {

    @Inject
    ClusteredJobRegistry registry;

    @Inject
    TaskContainer container;

    @Inject
    Event<ClusteredJob> jobEvent;

    @Inject
    ApplicationConfig appConfig;

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskURI;

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Test
    void testTaskGetsCancelledTimeout() throws InterruptedException {
        // given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(1000);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);
        Instant beforeTimeout = Instant.now();

        waitTillTasksAre(State.STOPPING, container, task);

        // then
        waitTillTasksAreFinishedWith(State.STOPPED, task);
        Instant afterTimeout = Instant.now();

        assertThat(Duration.between(beforeTimeout, afterTimeout).abs()).isCloseTo(timeoutValue, Duration.ofMillis(100));
    }

    @Test
    void testTimeoutJobIsEnlistedWithCorrectValues() throws InterruptedException {
        // given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(1000);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();

        graph.graphConfiguration.passMDCInRequestBody = true;
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTasksAre(State.STOPPING, container, task);
        Thread.sleep(50);
        ClusteredJobReference enlistedJob = registry.getById(referenceId);

        // then
        assertThat(enlistedJob).isNotNull();
        assertThat(enlistedJob.getOwner()).isNotNull().isNotBlank();
        assertThat(enlistedJob.getTaskName()).isEqualTo(task);
        assertThat(enlistedJob.getTelemetry()).isNotNull().isNotEmpty()
            .containsKey("traceparent");
    }

    @Test
    void testTimeoutJobIsDelisted() throws InterruptedException {
        //given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(300);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTasksAreFinishedWith(State.STOPPED, task);

        // then
        Thread.sleep(50);
        ClusteredJobReference enlistedJob = registry.getById(referenceId);
        assertThat(enlistedJob).isNull();
    }

    @Test
    void testTimeoutJobIsOKAfterCallback() throws InterruptedException {
        //given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(300);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.getVertices().get(task).remoteCancel = TestData.getStopRequestWithCallback("irrelevant");
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTasksAre(State.STOPPING, container, task);
        Thread.sleep(50);
        ClusteredJobReference enlistedJob = registry.getById(referenceId);
        assertThat(enlistedJob).isNotNull();

        waitTillTasksAreFinishedWith(State.STOPPED, task);

        // then
        Thread.sleep(timeoutValue.plus(200, ChronoUnit.MILLIS).toMillis());
        enlistedJob = registry.getById(referenceId);
        assertThat(enlistedJob).isNull();
    }

    @Test
    void testChangingOwnerDoesNotTriggerTimeout() throws InterruptedException {
        //given
        String task = taskID();
        String random = "another-random-node";
        Duration timeoutValue = Duration.ofMillis(300);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTasksAre(State.STOPPING, container, task);
        Thread.sleep(50);

        // when
        ClusteredJobReference reference = registry.getById(referenceId);
        assertThat(reference).isNotNull();

        var withNewOwner = reference.toBuilder().owner(random).build();

        ClusteredJobRegistryImpl registryImpl = (ClusteredJobRegistryImpl) registry;
        QuarkusTransaction.requiringNew().run(() -> registryImpl.forceCreate(withNewOwner));

        // then

        // wait for twice the limit
        assertThatThrownBy(() -> waitTillTasksAre(State.STOPPED,
            container,
            (int) (timeoutValue.toMillis()*2),
            MILLISECONDS,
            task))
            .isInstanceOf(AssertionError.class);

        assertThat(get(taskURI.getPath() + TaskEndpoint.GET_SPECIFIC_FMT.formatted(task)).as(TaskDTO.class))
            .isNotNull().extracting("state").isEqualTo(State.STOPPING);
    }

    @Test
    void testCreatingFromReferenceWorks() throws InterruptedException {
        //given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(300);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTasksAre(State.UP, container, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        // remove Job scheduled by /cancel
        waitTillTasksAre(State.STOPPING, container, task);
        Thread.sleep(50);

        var jobReference = registry.getById(referenceId);
        QuarkusTransaction.requiringNew().run(() -> registry.delete(jobReference.getId()));
        Thread.sleep(300); // waiting for async job to trigger and verify task is still stopping
        var taskDTO = get(taskURI.getPath() + TaskEndpoint.GET_SPECIFIC_FMT.formatted(task)).as(TaskDTO.class);
        assertThat(taskDTO).isNotNull().extracting("state").isEqualTo(State.STOPPING);

        // create job from reference and run it
        TimeoutCancelClusterJob timeoutCancelClusterJob = new TimeoutCancelClusterJob(new ClusteredJobReference(referenceId, jobReference.getOwner(), CJobOperation.CANCEL_TIMEOUT, new HashMap<>(jobReference.getTelemetry()), task));
        // run it from reference and verify it works
        jobEvent.fire(timeoutCancelClusterJob);

        //then
        waitTillTasksAreFinishedWith(State.STOPPED, task);
    }

    public static String taskID() {
        return "test-task-"+counter.getAndIncrement();
    }
}
