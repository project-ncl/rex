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
package org.jboss.pnc.rex.core;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.common.TransitionRecorder;
import org.jboss.pnc.rex.core.counter.Counter;
import org.jboss.pnc.rex.core.counter.MaxConcurrent;
import org.jboss.pnc.rex.core.counter.Running;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.TransitionTimeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.test.profile.WithoutTaskCleaning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.common.enums.Transition.ENQUEUED_to_STARTING;
import static org.jboss.pnc.rex.common.enums.Transition.NEW_to_ENQUEUED;
import static org.jboss.pnc.rex.common.enums.Transition.NEW_to_WAITING;
import static org.jboss.pnc.rex.common.enums.Transition.STARTING_to_UP;
import static org.jboss.pnc.rex.common.enums.Transition.UP_to_SUCCESSFUL;
import static org.jboss.pnc.rex.common.enums.Transition.WAITING_to_ENQUEUED;
import static org.jboss.pnc.rex.core.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.core.common.TestData.getAllParameters;
import static org.jboss.pnc.rex.core.common.TestData.getComplexGraph;
import static org.jboss.pnc.rex.core.common.TestData.getRequestWithNegativeCallback;

/**
 * Use this class instead of TaskContainerTest if you want to verify data in tasks after completion.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@TestProfile(WithoutTaskCleaning.class) // disable deletion of tasks
public class TaskContainerNoCleanTest {

    @Inject
    @Running
    Counter running;

    @Inject
    @MaxConcurrent
    Counter max;

    @Inject
    TaskContainerImpl container;

    @Inject
    TransitionRecorder recorder;

    @Inject
    TaskEndpoint taskEndpoint;

    @BeforeEach
    public void before() throws Exception {
        max.initialize(1000L);
        running.initialize(0L);
        container.getCache().clear();
    }

    @AfterEach
    public void after() throws InterruptedException {
        recorder.clear();
        Thread.sleep(100);
    }

    @Test
    void shouldFailATaskAndDependants() {
        CreateGraphRequest graphRequest = getComplexGraph(true);
        graphRequest.getVertices().get("e").remoteStart = getRequestWithNegativeCallback("e");

        taskEndpoint.start(graphRequest);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "a", "b", "c", "d", "f");
        waitTillTasksAreFinishedWith(State.FAILED, "e");
        waitTillTasksAreFinishedWith(State.STOPPED, "g", "h", "i", "j");

        Map<String, TaskDTO> tasks = taskEndpoint.getAll(getAllParameters()).stream()
                .collect(Collectors.toMap(TaskDTO::getName, Function.identity()));

        assertThat(tasks).hasSize(10);

        assertThat(tasks.get("e")).satisfies((task) -> {
            assertThat(task.getState()).isEqualTo(State.FAILED);
            assertThat(task.getStopFlag()).isEqualTo(StopFlag.UNSUCCESSFUL);
            assertThat(task.getServerResponses()).hasSize(2);
        });

        assertThat(tasks).extractingByKeys("g", "h", "i", "j").allSatisfy((task) -> {
            assertThat(task.getState()).isEqualTo(State.STOPPED);
            assertThat(task.getStopFlag()).isEqualTo(StopFlag.DEPENDENCY_FAILED);
        });
    }

    @Test
    void testTransitionTimes() throws InterruptedException {
        CreateGraphRequest request = getComplexGraph(true, true);
        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, request.getVertices().keySet().toArray(new String[0]));

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        Thread.sleep(100);

        // Extract transitions+timestamps and order by timestamps
        Map<String, List<TransitionTimeDTO>> times = all.stream()
                .collect(Collectors.toMap(
                        // key is task name
                        TaskDTO::getName,
                        // value is Tuple of transition and timestamp ordered by timestamp
                        TaskDTO::getTimestamps));
        assertThat(times.keySet()).containsExactlyInAnyOrderElementsOf(request.getVertices().keySet());

        containsInTimeOrder(times, "a", NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "b", NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "c", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "d", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "e", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "f", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "g", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "h", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "i", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);
        containsInTimeOrder(times, "j", NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL);

    }

    private static void containsInTimeOrder(Map<String, List<TransitionTimeDTO>> times, String task, Transition... transitions) {
        assertThat(times.get(task))
                .map(TransitionTimeDTO::getTransition)
                .containsExactlyElementsOf(Arrays.asList(transitions));
    }
}
