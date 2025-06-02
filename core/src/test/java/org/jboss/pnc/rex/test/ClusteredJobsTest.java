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
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.ClusteredJobRegistryImpl;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.core.jobs.TimeoutCancelClusterJob;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.rex.test.common.Assertions.*;

@Slf4j
@QuarkusTest
public class ClusteredJobsTest extends AbstractTest {

    @Inject
    ClusteredJobRegistry registry;

    @Inject
    TaskContainer container;

    @Inject
    Event<ClusteredJob> jobEvent;

    @Inject
    ApplicationConfig appConfig;

    @Inject
    ManagedExecutor executor;

    private Set<Future<?>> futures = new ConcurrentHashSet<>();

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskURI;

    @TestHTTPEndpoint(CallbackEndpoint.class)
    @TestHTTPResource
    URI callbackURI;

    private static final AtomicInteger counter = new AtomicInteger(0);

    @AfterEach
    void clean() {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        futures.clear();
    }

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
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);
        Instant beforeTimeout = Instant.now();

        waitTillTaskTransitionsInto(State.STOPPING, task);

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
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTaskTransitionsInto(State.STOPPING, task);
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
        waitTillTaskTransitionsInto(State.UP, task);

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
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTaskTransitionsInto(State.STOPPING, task);
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
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTaskTransitionsInto(State.STOPPING, task);
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
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        // remove Job scheduled by /cancel
        waitTillTaskTransitionsInto(State.STOPPING, task);
        Thread.sleep(50);

        var jobReference = registry.getById(referenceId);
        assertThat(jobReference).isNotNull();
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

    @Test
    void testHeartbeatBeats() throws InterruptedException {
        //given
        String taskId = taskID();
        Duration interval = Duration.ofMillis(100);
        var task = TestData.getMockTaskWithoutStart(taskId, Mode.ACTIVE);
        task.configuration = new ConfigurationDTO();
        task.configuration.heartbeatEnable = true;
        task.configuration.heartbeatInterval = interval;
        task.configuration.heartbeatToleranceThreshold = 1;

        var graph = CreateGraphRequest.builder().vertex(taskId, task).build();

        //when
        given()
                .contentType(ContentType.JSON)
                .body(graph)
                .when()
                .post(taskURI.getPath())
                .then()
                .statusCode(200);

        waitTillTaskTransitionsInto(State.UP, taskId);
        futures.add(executor.submit(() -> beat(taskId, interval)));

        Thread.sleep(200);

        //then
        Task internalTask = container.getTask(taskId);
        assertThat(internalTask).isNotNull();
        assertThat(internalTask.getState()).isEqualTo(State.UP);
        assertThat(internalTask.getHeartbeatMeta()).isNotNull();
        assertThat(internalTask.getHeartbeatMeta().getLastBeat()).isNotNull().isBefore(Instant.now());
    }

    @Test
    void testHeartbeatFails() throws InterruptedException {
        //given
        String taskId = taskID();
        Duration interval = Duration.ofMillis(100);
        var task = TestData.getMockTaskWithoutStart(taskId, Mode.ACTIVE);
        task.configuration = new ConfigurationDTO();
        task.configuration.heartbeatEnable = true;
        task.configuration.heartbeatInterval = interval;
        task.configuration.heartbeatToleranceThreshold = 1;

        var graph = CreateGraphRequest.builder().vertex(taskId, task).build();

        //when
        given()
                .contentType(ContentType.JSON)
                .body(graph)
                .when()
                .post(taskURI.getPath())
                .then()
                .statusCode(200);
        waitTillTaskTransitionsInto(State.UP, taskId);

        Instant startTimer = Instant.now();
        Task internalTask = waitTillTaskTransitionsInto(State.FAILED, taskId).get(0);
        Instant endTime = Instant.now();

        //then
        assertThat(internalTask).isNotNull();
        assertThat(internalTask.getState()).isEqualTo(State.FAILED);
        assertThat(internalTask.getHeartbeatMeta()).isNotNull();
        assertThat(internalTask.getServerResponses().get(internalTask.getServerResponses().size() - 1))
                .isNotNull().extracting(ServerResponse::getOrigin).isEqualTo(Origin.REX_HEARTBEAT_TIMEOUT);
        assertThat(Duration.between(startTimer, endTime)).isCloseTo(interval, Duration.ofMillis(50));
    }

    @RepeatedTest(100)
    void testHeartbeatFailsAfterCoupleBeats() throws InterruptedException {
        //given
        String taskId = taskID();

        Duration processingLeeway = Duration.ofMillis(200); // depends on the machine unfortunately
        Duration interval = Duration.ofMillis(100);

        var task = TestData.getMockTaskWithoutStart(taskId, Mode.ACTIVE);
        task.configuration = new ConfigurationDTO();
        task.configuration.heartbeatEnable = true;
        task.configuration.heartbeatInterval = interval;
        task.configuration.heartbeatToleranceThreshold = 1;

        var graph = CreateGraphRequest.builder().vertex(taskId, task).build();

        //when
        given()
                .contentType(ContentType.JSON)
                .body(graph)
                .when()
                .post(taskURI.getPath())
                .then()
                .statusCode(200);
        waitTillTaskTransitionsInto(State.UP, taskId);
        Instant startTimer = Instant.now();

        Future<?> beat = executor.submit(() -> beat(taskId, interval)); // start beating
        futures.add(beat);

        Duration sleeping = interval.multipliedBy(6);
        Thread.sleep(sleeping.toMillis()); // sleep for couple beats

        beat.cancel(true); // stop beeting

        Task internalTask = waitTillTaskTransitionsInto(State.FAILED, taskId).get(0);
        Instant endTime = Instant.now();

        //then
        assertThat(internalTask).isNotNull();
        assertThat(internalTask.getState()).isEqualTo(State.FAILED);
        assertThat(internalTask.getHeartbeatMeta()).isNotNull();
        assertThat(internalTask.getHeartbeatMeta().getLastBeat())// there should be at least 4/5 beats after startTime
                .isAfter(startTimer.plus(interval.multipliedBy(4)))
                .isBefore(endTime);
        assertThat(internalTask.getServerResponses().get(internalTask.getServerResponses().size() - 1))
                .isNotNull().extracting(ServerResponse::getOrigin).isEqualTo(Origin.REX_HEARTBEAT_TIMEOUT);
        assertThat(Duration.between(startTimer, endTime))
                .isCloseTo(sleeping.plus(interval.multipliedBy(2)), processingLeeway);
    }

    private void beat(String taskId, Duration interval) {
        try {
            while (true) {
                given()
                        .contentType(ContentType.JSON)
                        .post(callbackURI.getPath() + CallbackEndpoint.HEARTBEAT_FMT.formatted(taskId))
                        .then().statusCode(204);
                Thread.sleep(interval.toMillis());
            }
        } catch (InterruptedException e) {}
    }

    public static String taskID() {
        return "test-task-"+counter.getAndIncrement();
    }
}
