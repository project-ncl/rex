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

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.ErrorOption;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.GenericVertxHttpClient;
import org.jboss.pnc.rex.core.TaskContainerImpl;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.test.endpoints.TransitionRecorderEndpoint;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.jboss.pnc.rex.common.enums.Transition.ENQUEUED_to_STARTING;
import static org.jboss.pnc.rex.common.enums.Transition.NEW_to_ENQUEUED;
import static org.jboss.pnc.rex.common.enums.Transition.NEW_to_WAITING;
import static org.jboss.pnc.rex.common.enums.Transition.STARTING_to_UP;
import static org.jboss.pnc.rex.common.enums.Transition.STOPPING_TO_STOPPED;
import static org.jboss.pnc.rex.common.enums.Transition.STOP_REQUESTED_to_STOPPING;
import static org.jboss.pnc.rex.common.enums.Transition.UP_to_STOP_REQUESTED;
import static org.jboss.pnc.rex.common.enums.Transition.UP_to_SUCCESSFUL;
import static org.jboss.pnc.rex.common.enums.Transition.WAITING_to_ENQUEUED;
import static org.jboss.pnc.rex.common.enums.Transition.WAITING_to_STOPPED;
import static org.jboss.pnc.rex.test.common.Assertions.*;
import static org.jboss.pnc.rex.test.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.test.common.TestData.createMockTask;
import static org.jboss.pnc.rex.test.common.TestData.getAllParameters;
import static org.jboss.pnc.rex.test.common.TestData.getComplexGraph;
import static org.jboss.pnc.rex.test.common.TestData.getComplexGraphWithoutEnd;
import static org.jboss.pnc.rex.test.common.TestData.getNaughtyNotificationsRequest;
import static org.jboss.pnc.rex.test.common.TestData.getNotificationsRequest;
import static org.jboss.pnc.rex.test.common.TestData.getRequestWithStart;
import static org.jboss.pnc.rex.test.common.TestData.getStopRequest;
import static org.jboss.pnc.rex.test.common.TestData.getStopRequestWithCallback;

@Slf4j
@QuarkusTest
public class NotificationTest extends AbstractTest {

    @Inject
    TaskContainerImpl container;

    @Inject
    TaskEndpoint endpoint;

    @Inject
    TransitionRecorderEndpoint recorderEndpoint;

    @Inject
    GenericVertxHttpClient httpClient;

    @Inject
    ManagedExecutor executor;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Test
    void testNotifications() throws InterruptedException {
        CreateGraphRequest request = getComplexGraph(true, true);
        endpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, request.getVertices().keySet().toArray(new String[0]));
        
        Thread.sleep(100);
        Map<String, Set<Transition>> records = recorderEndpoint.getRecords();
        assertThat(records.keySet()).containsExactlyInAnyOrderElementsOf(request.getVertices().keySet());
        assertThat(records.get("a")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("b")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("c")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("d")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("e")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("f")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("g")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("h")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("i")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("j")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
    }

    @Test
    void testNotificationOnCancel() throws InterruptedException {
        CreateGraphRequest request = getComplexGraphWithoutEnd(true, true);
        endpoint.start(request);
        waitTillTaskTransitionsInto(State.UP, "a");
        waitTillTaskTransitionsInto(State.UP, "b");

        endpoint.cancel("a");
        endpoint.cancel("b");

        waitTillTasksAreFinishedWith(State.STOPPED, request.getVertices().keySet().toArray(new String[0]));

        Thread.sleep(100);
        Map<String, Set<Transition>> records = recorderEndpoint.getRecords();
        assertThat(records.keySet()).containsExactlyInAnyOrderElementsOf(request.getVertices().keySet());
        assertThat(records.get("a")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_STOP_REQUESTED, STOP_REQUESTED_to_STOPPING, STOPPING_TO_STOPPED));
        assertThat(records.get("b")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_STOP_REQUESTED, STOP_REQUESTED_to_STOPPING, STOPPING_TO_STOPPED));
        assertThat(records.get("c")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("d")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("e")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("f")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("g")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("h")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("i")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("j")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
    }

    /**
     * Graph (NCL-9060):
     * 1 -> 2
     * 2 -> 3
     * 2 -> 4 -> 5
     */
    @Test
    void testDeletionAfterNotification() throws InterruptedException {
        List<EdgeDTO> egdes = new ArrayList<>();

        CreateGraphRequest.CreateGraphRequestBuilder builder = CreateGraphRequest.builder();
        for (int i = 1; i <= 3; i++) {
            String name = String.valueOf(i);
            builder.vertex(name, CreateTaskDTO.builder()
                                                .name(name)
                                                .controllerMode(Mode.ACTIVE)
                                                .remoteStart(getRequestWithStart(name))
                                                .remoteCancel(getStopRequest(name))
                                                .callerNotifications(getNotificationsRequest())
                                                .build());
        }
        // don't complete 4 and 5
        for (int i = 4; i <= 5; i++) {
            String name = String.valueOf(i);
            builder.vertex(name, CreateTaskDTO.builder()
                                                .name(name)
                                                .controllerMode(Mode.ACTIVE)
                                                .remoteStart(TestData.getRequestWithoutStart(name))
                                                .remoteCancel(getStopRequest(name))
                                                .callerNotifications(getNotificationsRequest())
                                                .build());
        }

        egdes.add(new EdgeDTO("5","4"));
        egdes.add(new EdgeDTO("4","2"));
        egdes.add(new EdgeDTO("3","2"));
        egdes.add(new EdgeDTO("2","1"));
        builder.edges(egdes);
        CreateGraphRequest graph = builder.build();

        // when
        endpoint.start(graph);

        String[] taskNames = {"1", "2", "3"};
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskNames);
        Thread.sleep(100);

        Map<String, Set<Transition>> records = new HashMap<>(recorderEndpoint.getRecords());

        // then
        // 1st task
        assertThat(records)
            .extractingByKeys("1")
            .allSatisfy((firstTransitions) -> {
                assertThat(firstTransitions).containsExactlyInAnyOrderElementsOf(
                    Set.of(NEW_to_ENQUEUED,
                           ENQUEUED_to_STARTING,
                           STARTING_to_UP,
                           UP_to_SUCCESSFUL));
            });
        // 2nd and 3rd task
        assertThat(records)
            .extractingByKeys("2", "3")
            .allSatisfy((firstTransitions) -> {
                assertThat(firstTransitions).containsExactlyInAnyOrderElementsOf(
                    Set.of(NEW_to_WAITING,
                           WAITING_to_ENQUEUED,
                           ENQUEUED_to_STARTING,
                           STARTING_to_UP,
                           UP_to_SUCCESSFUL));
            });
        // 4th task
        assertThat(records)
            .extractingByKeys("4")
            .allSatisfy((firstTransitions) -> {
                assertThat(firstTransitions).containsExactlyInAnyOrderElementsOf(
                    Set.of(NEW_to_WAITING,
                           WAITING_to_ENQUEUED,
                           ENQUEUED_to_STARTING,
                           STARTING_to_UP));
            });
        // 5th task
        assertThat(records)
            .extractingByKeys("5")
            .allSatisfy((firstTransitions) -> {
                assertThat(firstTransitions).containsExactlyInAnyOrderElementsOf(
                    Set.of(NEW_to_WAITING));
            });

        Thread.sleep(100);
        // only 3rd task has been deleted
        Set<TaskDTO> all = endpoint.getAll(getAllParameters(), null);
        assertThat(all)
            .extracting("name")
                .containsExactlyInAnyOrder("1", "2", "4", "5");

        // then complete 4
        executor.runAsync(() -> callbackEndpoint.succeed("4", "the-result", ErrorOption.IGNORE));
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "4");
        // the tasks shouldn't be deleted yet
        Set<TaskDTO> allWhenCompleted4 = endpoint.getAll(getAllParameters(), null);
        assertThat(allWhenCompleted4)
            .extracting("name")
            .containsExactlyInAnyOrder("1", "2", "4", "5");

        // then complete 5
        executor.runAsync(() -> callbackEndpoint.succeed("5", "the-result", ErrorOption.IGNORE));
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "5");
        Thread.sleep(100);

        // make sure all tasks have been deleted
        Set<TaskDTO> allWhenCompleted5 = endpoint.getAll(getAllParameters(), null);
        assertThat(allWhenCompleted5.size()).isEqualTo(0);

    }

    @Test
    void testDeletionAfterNotificationWithVeryComplexGraph() throws InterruptedException {
        // to make the test deterministic
        int seed = 1000;
        CreateGraphRequest graph = generateDAG(seed, 2, 10, 5, 10, 0.7F, true);

        // when
        endpoint.start(graph);

        String[] taskNames = graph.getVertices().keySet().toArray(new String[0]);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskNames);
        Thread.sleep(100);

        Map<String, Set<Transition>> records = new HashMap<>(recorderEndpoint.getRecords());

        // then
        var firstTasks = new String[]{"15","52","75","76","77","78","79","80"};
        assertThat(records)
                .extractingByKeys(firstTasks)
                .allSatisfy((firstTransitions) -> {
                    assertThat(firstTransitions).containsExactlyInAnyOrderElementsOf(
                            Set.of(NEW_to_ENQUEUED,
                                    ENQUEUED_to_STARTING,
                                    STARTING_to_UP,
                                    UP_to_SUCCESSFUL));
                });

        // remove first tasks
        records.keySet().removeAll(Arrays.asList(firstTasks));
        assertThat(records)
                .allSatisfy((remainingTasks, transitions) -> {
                    assertThat(transitions).containsExactlyInAnyOrderElementsOf(
                            Set.of(NEW_to_WAITING,
                                    WAITING_to_ENQUEUED,
                                    ENQUEUED_to_STARTING,
                                    STARTING_to_UP,
                                    UP_to_SUCCESSFUL));
                });
        Thread.sleep(200);

        assertThat(endpoint.getAll(getAllParameters(), null)).isEmpty();
    }

    @Test
    void testDeletionDoesntHappenOnFailedFinalNotification() throws InterruptedException {
        String taskName = "task";
        var okNotificationTask = createMockTask(
                taskName,
                Mode.ACTIVE,
                getRequestWithStart(taskName),
                getStopRequestWithCallback(taskName),
                getNotificationsRequest());
        var failingNotificationTask = createMockTask(
                taskName,
                Mode.ACTIVE,
                getRequestWithStart(taskName),
                getStopRequestWithCallback(taskName),
                getNaughtyNotificationsRequest());

        var reqGoodNot = CreateGraphRequest.builder().vertex(taskName, okNotificationTask).build();
        var reqBadNot = CreateGraphRequest.builder().vertex(taskName, failingNotificationTask).build();

        endpoint.start(reqGoodNot);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskName);
        Thread.sleep(100);

        // assert task was deleted
        assertThat(endpoint.getAll(getAllParameters(), null)).extracting(TaskDTO::getName).doesNotContain(taskName);

        endpoint.start(reqBadNot);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskName);
        Thread.sleep(100);

        assertThat(endpoint.getSpecific(taskName)).isNotNull();
    }

    @Test
    void testConstraintIsClearedAfterFailedNotification() throws InterruptedException {
        String taskName = "task";
        String otherTaskName = "other-task";
        String sharedConstraint = "constraint";

        var failingNotificationTask = createMockTask(
            taskName,
            Mode.ACTIVE,
            getRequestWithStart(taskName),
            getStopRequestWithCallback(taskName),
            getNaughtyNotificationsRequest()).toBuilder()
            .constraint(sharedConstraint)
            .build();
        var runAfterTask = createMockTask(
            otherTaskName,
            Mode.ACTIVE,
            getRequestWithStart(taskName),
            getStopRequestWithCallback(taskName),
            getNotificationsRequest()).toBuilder()
            .constraint(sharedConstraint)
            .build();

        var reqFailNot = CreateGraphRequest.builder().vertex(taskName, failingNotificationTask).build();
        var reqAfter = CreateGraphRequest.builder().vertex(otherTaskName, runAfterTask).build();

        endpoint.start(reqFailNot);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskName);
        Thread.sleep(100);

        // assert task was not deleted
        assertThat(endpoint.getAll(getAllParameters(), null)).extracting(TaskDTO::getName).contains(taskName);

        // should proceed because shared constraint should be cleared
        assertThatNoException().isThrownBy(() -> endpoint.start(reqAfter));
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, otherTaskName);
        Thread.sleep(100);

        assertThat(endpoint.getAll(getAllParameters(), null)).extracting(TaskDTO::getName).doesNotContain(otherTaskName);

    }
}
