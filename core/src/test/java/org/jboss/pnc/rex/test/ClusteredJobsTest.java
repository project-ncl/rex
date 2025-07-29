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
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

    // depends on the machine unfortunately
    public static final Duration PROCESSING_LEEWAY = Duration.ofMillis(200);

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
        task.configuration.heartbeatToleranceThreshold = 0;

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
        assertThat(Duration.between(startTimer, endTime)).isBetween(interval, interval.plus(PROCESSING_LEEWAY));
    }

    @Test
    void testHeartbeatFailsAfterCoupleBeats() throws InterruptedException {
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
        Future<?> beat = executor.submit(() -> beat(taskId, interval)); // start beating
        futures.add(beat);

        Duration sleeping = interval.multipliedBy(6);
        Thread.sleep(sleeping.toMillis()); // sleep for couple beats

        beat.cancel(true); // stop beating

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
                .isCloseTo(sleeping.plus(interval.multipliedBy(2)), PROCESSING_LEEWAY);
    }

    @Test
    void testHeartbeatFailureThreshold() {
        //given
        String taskId = taskID();

        Duration interval = Duration.ofMillis(100);
        int failureTolerance = 5;

        var task = TestData.getMockTaskWithoutStart(taskId, Mode.ACTIVE);
        task.configuration = new ConfigurationDTO();
        task.configuration.heartbeatEnable = true;
        task.configuration.heartbeatInterval = interval;
        task.configuration.heartbeatToleranceThreshold = failureTolerance; // increased tolerance

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
        assertThat(Duration.between(startTimer, endTime))
                .isCloseTo(interval.multipliedBy(failureTolerance + 1), PROCESSING_LEEWAY);
    }

    @Test
    void testHeartbeatInitialDelay() {
        //given
        String taskId = taskID();
        Duration interval = Duration.ofMillis(100);
        Duration initialDelay = Duration.ofMillis(300);

        var task = TestData.getMockTaskWithoutStart(taskId, Mode.ACTIVE);
        task.configuration = new ConfigurationDTO();
        task.configuration.heartbeatEnable = true;
        task.configuration.heartbeatInterval = interval;
        task.configuration.heartbeatInitialDelay = initialDelay;
        task.configuration.heartbeatToleranceThreshold = 0;

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
        Duration officialDelay = initialDelay.plus(interval);
        assertThat(Duration.between(startTimer, endTime)).isGreaterThan(officialDelay).isLessThan(officialDelay.plus(PROCESSING_LEEWAY));
    }

    @Test
    void testConcurrentHeartbeatFails() {
        //given
        String taskId1 = taskID();
        String taskId2 = taskID();
        String taskId3 = taskID();
        var task1 = TestData.getMockTaskWithoutStart(taskId1, Mode.ACTIVE);
        var task2 = TestData.getMockTaskWithoutStart(taskId2, Mode.ACTIVE);
        var task3 = TestData.getMockTaskWithoutStart(taskId3, Mode.ACTIVE);
        var graph = CreateGraphRequest.builder()
                .graphConfiguration(ConfigurationDTO.builder()
                        .heartbeatEnable(true)
                        .heartbeatInterval(Duration.ofMillis(100))
                        .heartbeatToleranceThreshold(2)
                        .heartbeatInitialDelay(Duration.ofMillis(200))
                        .build())
                .vertex(taskId1, task1)
                .vertex(taskId2, task2)
                .vertex(taskId3, task3)
                .build();

        //when
        given()
                .contentType(ContentType.JSON)
                .body(graph)
                .when()
                .post(taskURI.getPath())
                .then()
                .statusCode(200);

        //then
        Task task1fail = waitTillTaskTransitionsInto(State.FAILED, taskId1).getFirst();
        Task task2fail = waitTillTaskTransitionsInto(State.FAILED, taskId2).getFirst();
        Task task3fail = waitTillTaskTransitionsInto(State.FAILED, taskId3).getFirst();

        assertThat(task1fail).isNotNull();
        assertThat(task1fail.getServerResponses()).isNotEmpty().anyMatch(serverResponse -> serverResponse.getOrigin().equals(Origin.REX_HEARTBEAT_TIMEOUT));
        assertThat(task2fail).isNotNull();
        assertThat(task2fail.getServerResponses()).isNotEmpty().anyMatch(serverResponse -> serverResponse.getOrigin().equals(Origin.REX_HEARTBEAT_TIMEOUT));
        assertThat(task3fail).isNotNull();
        assertThat(task3fail.getServerResponses()).isNotEmpty().anyMatch(serverResponse -> serverResponse.getOrigin().equals(Origin.REX_HEARTBEAT_TIMEOUT));

    }

    private void beat(String taskId, Duration interval) {
        try {
            while (true) {
                Instant startTime = Instant.now();
                given()
                        .contentType(ContentType.JSON)
                        .post(callbackURI.getPath() + CallbackEndpoint.HEARTBEAT_FMT.formatted(taskId))
                        .then().statusCode(204);
                Duration aligned = interval.minus(Duration.between(startTime, Instant.now()));
                Thread.sleep(aligned.toMillis());
            }
        } catch (InterruptedException e) {}
    }

    public static String taskID() {
        return "test-task-"+counter.getAndIncrement();
    }
}
