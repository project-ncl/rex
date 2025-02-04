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
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.model.TransitionTime;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.test.endpoints.TransitionRecorderEndpoint;
import org.jboss.pnc.rex.test.profile.WithoutTaskCleaning;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.common.enums.Transition.*;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.TestData.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@TestProfile(WithoutTaskCleaning.class) // disable deletion of tasks
public class NotificationDisableCleanTest extends AbstractTest {

    @Inject
    TaskEndpoint endpoint;

    @Inject
    TransitionRecorderEndpoint recorderEndpoint;

    @Test
    void testRecordedBodies() throws InterruptedException {
        CreateGraphRequest request = getComplexGraph(true, true);
        endpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, request.getVertices().keySet().toArray(new String[0]));

        Thread.sleep(100);
        Set<TaskDTO> all = endpoint.getAll(TestData.getAllParameters(), null);
        Predicate<TaskDTO> sizePredicate = (task) -> task.getServerResponses() != null
                && task.getServerResponses().size() == 2;
        Predicate<TaskDTO> responsePredicate = (task) -> {
            var responses = task.getServerResponses();
            boolean firstBody = responses.stream().anyMatch((response ->
                    response.getState() == State.STARTING
                    && response.getBody() instanceof Map
                    && !((Map<String, String>) response.getBody()).get("task").isEmpty()));
            boolean secondBody = responses.stream().anyMatch((response ->
                    response.getState() == State.UP
                    && response.getBody() instanceof String
                    && response.getBody().equals("ALL IS OK")));
            return firstBody && secondBody;
        };
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(sizePredicate);
        assertThat(all).allMatch(responsePredicate);
    }


    @Test
    void testDependenciesAreStartedAfterSuccessfulNotification() throws InterruptedException {
        // with
        CreateGraphRequest request = getComplexGraph(true, true);
        request.graphConfiguration = ConfigurationDTO.builder()
                .delayDependantsForFinalNotification(true)
                .build();

        // when
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

        Map<String, Map<Transition, Instant>> recordsWithTimestamps = recorderEndpoint.getRecordsWithTimestamps()
                .entrySet()
                .stream()
                .collect(toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .collect(toMap(
                                                TransitionTime::getTransition,
                                                TransitionTime::getTime)
                                        )
                        )
                );

        // then

        // assert that waitForFinalNotification had an impact
        // edges from TestData.getComplexGraph()
        assertSuccessReceivedBeforeQueueing("c", "a", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("d", "a", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("d", "b", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("e", "d", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("e", "b", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("f", "c", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("g", "e", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("h", "e", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("h", "b", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("i", "f", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("i", "g", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("j", "g", recordsWithTimestamps);
        assertSuccessReceivedBeforeQueueing("j", "h", recordsWithTimestamps);

    }

    @Test
    void testDependenciesFailOnUnsuccessfulNotificationWithDependantWaiting() throws InterruptedException {
        // with
        CreateGraphRequest request = getComplexGraph(true, true);
        request.graphConfiguration = ConfigurationDTO.builder()
                .delayDependantsForFinalNotification(true)
                .build();

        request.getVertices().get("a").callerNotifications = getNaughtyNotificationsRequest();
        request.getVertices().get("b").callerNotifications = getNaughtyNotificationsRequest();

        // when
        endpoint.start(request);
        waitTillTasksAreFinishedWith(State.STOPPED, request.getVertices()
                .keySet().stream()
                .filter(task -> !List.of("a", "b").contains(task)) // both A and B will be SUCCESS; others STOPPED
                .toArray(String[]::new));
        Thread.sleep(100);
        Set<TaskDTO> all = endpoint.getAll(getAllParameters(), null);

        // then
        assertThat(all).hasSize(10);
        Map<String, TaskDTO> indexedTasks = all.stream().collect(toMap(
                TaskDTO::getName, Function.identity()
        ));

        // 'a' and 'b' finished fine but their final notification failed
        assertThat(indexedTasks).extractingByKeys("a", "b").allSatisfy(task -> {
            assertThat(task.getState() == State.SUCCESSFUL).isTrue();
        });

        // other dependent(+transitive) tasks should fail and have an appropriate DEPENDENCY_NOTIFY_FAILED StopFlag
        assertThat(indexedTasks).extractingByKeys("c", "d", "e", "f", "g", "h", "i", "j").allSatisfy(task -> {
            assertThat(task.getState() == State.STOPPED).isTrue();
            assertThat(task.getStopFlag() == StopFlag.DEPENDENCY_NOTIFY_FAILED).isTrue();
        });
    }

    /**
     * Asserts that Rex waited for SUCCESSFUL notification before queueing/starting dependent Tasks
     */
    private void assertSuccessReceivedBeforeQueueing(String dependant, String dependency, Map<String, Map<Transition, Instant>> recordsWithTimestamps) {

        Map<Transition, Instant> dependantTransitions = recordsWithTimestamps.get(dependant);
        Map<Transition, Instant> dependencyTransitions = recordsWithTimestamps.get(dependency);

        assertThat(dependencyTransitions.get(UP_to_SUCCESSFUL)).isBefore(dependantTransitions.get(WAITING_to_ENQUEUED));
    }

}
