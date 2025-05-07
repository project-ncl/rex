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
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.ErrorOption;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.api.RollbackManager;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.TransitionTimeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.test.common.TransitionRecorder;
import org.jboss.pnc.rex.test.profile.WithoutTaskCleaning;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTaskTransitionsInto;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.test.common.TestData.*;

@QuarkusTest
@TestProfile(WithoutTaskCleaning.class) // disable deletion of tasks
public class RollbackProcessTest extends AbstractTest {

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    TaskContainer taskContainer;

    @Inject
    RollbackManager rollbackManager;

    @Inject
    TransitionRecorder recorder;

    @Inject
    ManagedExecutor executor;


    @Test
    public void testSingleTaskFastRollback() {
        //with
        String trigger = "test-task";
        var triggerTask = getMockTaskWithStart(trigger, Mode.ACTIVE, true, false);
        triggerTask.milestoneTask = trigger; //reset from itself
        triggerTask.remoteStart = getRequestWithNegativeCallback("payload");
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();
        CreateGraphRequest graph = CreateGraphRequest.builder().vertex(trigger, triggerTask).build();

        //when
        taskEndpoint.start(graph);
        waitTillTasksAreFinishedWith(State.FAILED, trigger);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());
        assertThat(tasks).hasSize(1);

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();

        //then
        assertThat(triggerRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_ROLLBACK_TRIGGERED,
                        Transition.ROLLBACK_TRIGGERED_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED);
    }

    @Test
    public void testSingleTaskRollbackWithCallback() {
        //with
        String trigger = "test-task";
        
        var triggerTask = getMockTaskWithStart(trigger, Mode.ACTIVE, true, true);
        triggerTask.milestoneTask = trigger; //reset from itself
        triggerTask.remoteStart = getRequestWithNegativeCallback("payload");
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();
        CreateGraphRequest graph = getRequestFromSingleTask(triggerTask);

        //when
        taskEndpoint.start(graph);
        waitTillTasksAreFinishedWith(State.FAILED, trigger);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());
        assertThat(tasks).hasSize(1);

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();

        //then
        assertThat(triggerRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_ROLLBACK_TRIGGERED,
                        Transition.ROLLBACK_TRIGGERED_to_ROLLBACK_REQUESTED,
                        Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                        Transition.ROLLINGBACK_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED);
        }

    @Test
    public void testSimpleRollbackFromDependency() {
        //with
        String trigger = "fail-task";
        String dependency = "dep-task";

        var triggerTask = getMockTaskWithStart(trigger, Mode.ACTIVE, true, false);
        triggerTask.milestoneTask = dependency; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();
        triggerTask.remoteStart = getRequestWithNegativeCallback("payload");

        var dependencyTask = getMockTaskWithStart(dependency, Mode.ACTIVE, true, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(dependency).build())
                .vertex(trigger, triggerTask)
                .vertex(dependency, dependencyTask).build();

        // when
        taskEndpoint.start(graph);
        waitTillTasksAreFinishedWith(State.FAILED, trigger);

        // then
        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());
        assertThat(tasks).hasSize(2);

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().orElseThrow().getTimestamps();
        var dependencyRecords = tasks.stream().filter(task -> task.getName().equals(dependency)).findFirst().orElseThrow().getTimestamps();

        assertMilestoneTrigger(triggerRecords, false, true);;

        assertRolledBackTask(dependencyRecords, false, false);
    }

    @Test
    public void testSimpleRollbackFromDependencyWithCallback() {
        //with
        String trigger = "fail-task";
        String dependency = "dep-task";

        var triggerTask = getMockTaskWithStart(trigger, Mode.ACTIVE, false, false);
        triggerTask.milestoneTask = dependency; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();
        triggerTask.remoteStart = getRequestWithNegativeCallback("payload");

        var dependencyTask = getMockTaskWithStart(dependency, Mode.ACTIVE, false, true);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(dependency).build())
                .vertex(trigger, triggerTask)
                .vertex(dependency, dependencyTask).build();

        // when
        taskEndpoint.start(graph);
        waitTillTasksAreFinishedWith(State.FAILED, trigger);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());
        assertThat(tasks).hasSize(2);

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var dependencyRecords = tasks.stream().filter(task -> task.getName().equals(dependency)).findFirst().get().getTimestamps();

        // then
        assertThat(triggerRecords)
                .hasSize(12)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_ROLLBACK_TRIGGERED,
                        Transition.ROLLBACK_TRIGGERED_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED);

        assertRolledBackTask(dependencyRecords, true, false);
    }

    @Test
    public void shouldRollbackFailedSiblingTask() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String failedState = "will-fail-task";

        // trigger task stops at UP for easier control
        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var failedStateTask = getMockTaskWithStart(failedState, Mode.ACTIVE, false, false);
        failedStateTask.remoteStart = getRequestWithNegativeCallback("payload");

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(failedState).target(milestone).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(failedState, failedStateTask)
                .build();

        // when
        taskEndpoint.start(graph);
        waitTillTaskTransitionsInto(State.FAILED, failedState);

        // trigger milestone
        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE));

        waitTillTaskTransitionsInto(State.FAILED, failedState, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());
        assertThat(tasks).hasSize(3);

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var failedStateTaskRecords = tasks.stream().filter(task -> task.getName().equals(failedState)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords,false, false);

        assertThat(failedStateTaskRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED,
                        Transition.FAILED_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED);
    }

    @Test
    public void shouldRollbackUpStateSiblingTask() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String upState = "up-task";

        //
        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var upStateTask = getMockTaskWithoutStart(upState, Mode.ACTIVE, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(upState).target(milestone).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(upState, upStateTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, upState);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var upStateRecords = tasks.stream().filter(task -> task.getName().equals(upState)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertThat(upStateRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP);
    }

    @Test
    public void shouldRollbackSuccessfulSiblingTask() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String success = "success-task";

        //
        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var successTask = getMockTaskWithStart(success, Mode.ACTIVE, false, true);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(success).target(milestone).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(success, successTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.SUCCESSFUL, success);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.SUCCESSFUL, success, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var successRecords = tasks.stream().filter(task -> task.getName().equals(success)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, true);

        assertThat(successRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_SUCCESSFUL,
                        Transition.SUCCESSFUL_to_ROLLBACK_REQUESTED,
                        Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                        Transition.ROLLINGBACK_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_SUCCESSFUL);
    }

    @Test
    public void shouldRollbackWaitingSiblingTask() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String up = "up-task";
        String waiting = "waiting-task";
        
        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var upTask = getMockTaskWithoutStart(up, Mode.ACTIVE, false);
        
        var waitingTask = getMockTaskWithStart(waiting, Mode.ACTIVE, false, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(up).target(milestone).build())
                .edge(EdgeDTO.builder().source(waiting).target(up).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(up, upTask)
                .vertex(waiting, waitingTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, up);
        waitTillTaskTransitionsInto(State.WAITING, waiting);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.WAITING, waiting, 2);
        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var upRecords = tasks.stream().filter(task -> task.getName().equals(up)).findFirst().get().getTimestamps();
        var waitingRecords = tasks.stream().filter(task -> task.getName().equals(waiting)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertThat(upRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP);

        assertThat(waitingRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING);
    }

    @Test
    public void shouldIgnoreCancelledTask() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String cancelled = "cancelled-task";
        String cancelledDep = "cancelled-dep-task";

        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var cancelledTask = getMockTaskWithStart(cancelled, Mode.ACTIVE, false, true);

        var cancelledDepTask = getMockTaskWithoutStart(cancelledDep, Mode.ACTIVE, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(cancelled).target(milestone).build())
                .edge(EdgeDTO.builder().source(cancelled).target(cancelledDep).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(cancelled, cancelledTask)
                .vertex(cancelledDep, cancelledDepTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, trigger);
        waitTillTaskTransitionsInto(State.WAITING, cancelled);

        executor.runAsync(() -> taskEndpoint.cancel(cancelledDep)); // cancel dependency of cancelled-task
        waitTillTaskTransitionsInto(State.STOPPED, cancelled);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var cancelledRecords = tasks.stream().filter(task -> task.getName().equals(cancelled)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        // has to be unaffected
        assertThat(cancelledRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_STOPPED);
    }

    /**
     * Rollback process cannot involve a STOPPED task that failed because a dependency failed AND the dependency is NOT
     * involved in the process (the graph of rollback tasks).
     */
    @Test
    public void shouldIgnoreDepFailedTaskFromOtherBranch() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String stopped = "stopped-task";
        String failed = "failed-dep-task";

        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var stoppedTask = getMockTaskWithStart(stopped, Mode.ACTIVE, false, true);

        var failedTask = getMockTaskWithoutStart(failed, Mode.ACTIVE, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(stopped).target(milestone).build())
                .edge(EdgeDTO.builder().source(stopped).target(failed).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(stopped, stoppedTask)
                .vertex(failed, failedTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, trigger);
        waitTillTaskTransitionsInto(State.WAITING, stopped);

        executor.runAsync(() -> callbackEndpoint.fail(failed, "lul", ErrorOption.IGNORE)); // fail dependency of stopped-task
        waitTillTaskTransitionsInto(State.FAILED, failed);
        waitTillTaskTransitionsInto(State.STOPPED, stopped);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var stoppedRecords = tasks.stream().filter(task -> task.getName().equals(stopped)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertThat(stoppedRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_STOPPED);
    }

    @Test
    public void shouldNotIgnoreDepFailedTaskFromSameBranch() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String stopped = "stopped-task";
        String failed = "failed-dep-task";

        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var stoppedTask = getMockTaskWithStart(stopped, Mode.ACTIVE, false, true);

        var failedTask = getMockTaskWithoutStart(failed, Mode.ACTIVE, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(stopped).target(failed).build())
                .edge(EdgeDTO.builder().source(failed).target(milestone).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(stopped, stoppedTask)
                .vertex(failed, failedTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, trigger);
        waitTillTaskTransitionsInto(State.WAITING, stopped);

        executor.runAsync(() -> callbackEndpoint.fail(failed, "lul", ErrorOption.IGNORE)); // fail dependency of stopped-task
        waitTillTaskTransitionsInto(State.FAILED, failed);
        waitTillTaskTransitionsInto(State.STOPPED, stopped);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var stoppedRecords = tasks.stream().filter(task -> task.getName().equals(stopped)).findFirst().get().getTimestamps();
        var failedRecords = tasks.stream().filter(task -> task.getName().equals(failed)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertThat(failedRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP,
                        Transition.UP_to_FAILED,
                        Transition.FAILED_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_ENQUEUED,
                        Transition.ENQUEUED_to_STARTING,
                        Transition.STARTING_to_UP);

        assertThat(stoppedRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_STOPPED,
                        Transition.STOPPED_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW,
                        Transition.NEW_to_WAITING);
    }


    @Test
    public void shouldIgnoreNotifyFailedTaskFromOtherBranch() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String stopped = "stopped-task";
        String notifyFailed = "notify-failed-task";

        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var stoppedTask = getMockTaskWithStart(stopped, Mode.ACTIVE, false, true);

        var notifyFailedTask = getMockTaskWithoutStart(notifyFailed, Mode.ACTIVE, true);
        notifyFailedTask.callerNotifications = TestData.getNaughtyNotificationsRequest();
        notifyFailedTask.configuration = ConfigurationDTO.builder().delayDependantsForFinalNotification(true).build();

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(stopped).target(milestone).build())
                .edge(EdgeDTO.builder().source(stopped).target(notifyFailed).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(stopped, stoppedTask)
                .vertex(notifyFailed, notifyFailedTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, trigger);
        waitTillTaskTransitionsInto(State.WAITING, stopped);
        waitTillTaskTransitionsInto(State.UP, notifyFailed);

        // succeed dependency of stopped-task but with failing final notification
        executor.runAsync(() -> callbackEndpoint.succeed(notifyFailed, "lul", ErrorOption.IGNORE));

        waitTillTaskTransitionsInto(State.SUCCESSFUL, notifyFailed);
        Task stoppedTaskModel = waitTillTaskTransitionsInto(State.STOPPED, stopped).get(0);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var stoppedRecords = tasks.stream().filter(task -> task.getName().equals(stopped)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertThat(stoppedTaskModel.getStoppedCause()).isEqualTo(notifyFailed);
        assertThat(stoppedRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_WAITING,
                        Transition.WAITING_to_STOPPED);
    }

    /**
     * Assures that if a task is created with Mode.IDLE, the mode is retained even after rollback
     */
    @Test
    public void shouldRollbackInactiveTasksWithoutStartingThem() {
        //with
        String trigger = "trigger-task";
        String milestone = "milestone-task";
        String idle = "idle-task";
        String success = "success-task";

        var triggerTask = getMockTaskWithoutStart(trigger, Mode.ACTIVE, false);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, false);

        var idleTask = getMockTaskWithStart(idle, Mode.IDLE, false, true);

        var successTask = getMockTaskWithStart(success, Mode.ACTIVE, true, false);


        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(trigger).target(milestone).build())
                .edge(EdgeDTO.builder().source(success).target(milestone).build())
                .edge(EdgeDTO.builder().source(idle).target(success).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(idle, idleTask)
                .vertex(success, successTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, trigger);
        waitTillTaskTransitionsInto(State.SUCCESSFUL, success);

        executor.runAsync(() -> callbackEndpoint.fail(trigger, "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.UP, trigger, 2);
        waitTillTaskTransitionsInto(State.SUCCESSFUL, success, 2);
        waitTillTaskTransitionsInto(State.NEW, idle, 1); // transition (->NEW) occurs just once

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var successRecords = tasks.stream().filter(task -> task.getName().equals(success)).findFirst().get().getTimestamps();
        var idleRecords = tasks.stream().filter(task -> task.getName().equals(idle)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, false, false);

        assertRolledBackTask(milestoneRecords, false, false);

        assertRolledBackTask(successRecords, false, false, true);

        assertThat(idleRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(
                        Transition.NEW_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW);
    }

    @Test
    public void testCallbackPropagation() {
        //with
        String trigger = "trigger-task"; // callback
        String milestone = "milestone-task"; // callback
        String noCallback = "no-callback-task"; // no-callback

        var triggerTask = getMockTaskWithStart(trigger, Mode.ACTIVE, false, true);
        triggerTask.milestoneTask = milestone; //rollback from dependency
        triggerTask.remoteStart = getRequestWithNegativeCallback("payload");
        triggerTask.configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        var milestoneTask = getMockTaskWithStart(milestone, Mode.ACTIVE, false, true);

        var noCallbackTask = getMockTaskWithStart(noCallback, Mode.ACTIVE, false, false);

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source(noCallback).target(milestone).build())
                .edge(EdgeDTO.builder().source(trigger).target(noCallback).build())
                .vertex(trigger, triggerTask)
                .vertex(milestone, milestoneTask)
                .vertex(noCallback, noCallbackTask)
                .build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.FAILED, trigger, 1);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        var triggerRecords = tasks.stream().filter(task -> task.getName().equals(trigger)).findFirst().get().getTimestamps();
        var milestoneRecords = tasks.stream().filter(task -> task.getName().equals(milestone)).findFirst().get().getTimestamps();
        var noCallbackRecords = tasks.stream().filter(task -> task.getName().equals(noCallback)).findFirst().get().getTimestamps();

        // then
        assertMilestoneTrigger(triggerRecords, true, true);

        assertRolledBackTask(noCallbackRecords, false, true, true);

        assertRolledBackTask(milestoneRecords, true, true);
    }

    @Test
    public void testComplexGraphCallbacks() {
        //with
        CreateGraphRequest graph = getComplexGraph(true);

        graph.getVertices().forEach((ign, task) -> {
            task.remoteRollback = getRequestForRollback();
        });

        graph.getVertices().get("h").remoteStart = getRequestWithoutStart("payload");
        graph.getVertices().get("h").milestoneTask = "d";
        graph.getVertices().get("h").configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.SUCCESSFUL, "i");

        executor.runAsync(() -> callbackEndpoint.fail("h", "lol", ErrorOption.IGNORE)); //trigger rollback

        waitTillTaskTransitionsInto(State.WAITING, "j",2);
        waitTillTaskTransitionsInto(State.SUCCESSFUL, "i",2);

        var tasks = taskEndpoint.getAll(getAllParameters(), List.of());

        // then
        assertThat(tasks.stream().filter(task -> Set.of("e", "g").contains(task.getName())).map(TaskDTO::getTimestamps).toList())
                .allSatisfy(records -> {
                    assertRolledBackTask(records, true, true, true);
                });
        assertThat(tasks.stream().filter(task -> "i".equals(task.getName())).map(TaskDTO::getTimestamps).toList())
                .allSatisfy(records -> {
                    assertRolledBackTask(records, true, false, true);
                });
        assertThat(tasks.stream().filter(task -> "d".equals(task.getName())).map(TaskDTO::getTimestamps).toList())
                .allSatisfy(records -> {
                    assertThat(records)
                            .extracting(TransitionTimeDTO::getTransition)
                            .containsExactly(
                            Transition.NEW_to_WAITING,
                            Transition.WAITING_to_ENQUEUED,
                            Transition.ENQUEUED_to_STARTING,
                            Transition.STARTING_to_UP,
                            Transition.UP_to_SUCCESSFUL,
                            Transition.SUCCESSFUL_to_TO_ROLLBACK,
                            Transition.TO_ROLLBACK_to_ROLLBACK_REQUESTED,
                            Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                            Transition.ROLLINGBACK_to_ROLLEDBACK,
                            Transition.ROLLEDBACK_to_NEW,
                            Transition.NEW_to_ENQUEUED,
                            Transition.ENQUEUED_to_STARTING,
                            Transition.STARTING_to_UP,
                            Transition.UP_to_SUCCESSFUL
                    );
                });

    }

    @Test
    public void testGigaGraphRandomCallbackSetup() {
        // with
        // to make the test deterministic
        int seed = 1000;
        CreateGraphRequest graph = generateDAG(seed, 2, 10, 5, 10, 0.7F, false);

        graph.getVertices().get("0").milestoneTask = "77";
        graph.getVertices().get("0").remoteStart = getRequestWithoutStart("payload");
        graph.getVertices().get("0").configuration = ConfigurationDTO.builder().rollbackLimit(1).build();

        // create mayhem
        graph.getVertices().forEach((name, task) -> {
            if (Integer.parseInt(name) % 2 == 0) {
                task.remoteRollback = getRequestForRollback();
            } else {
                task.remoteRollback = null;
            }
        });

        // when
        taskEndpoint.start(graph);

        waitTillTaskTransitionsInto(State.UP, "0",1);
        executor.runAsync(() -> callbackEndpoint.fail("0", "lol", ErrorOption.IGNORE)); //trigger rollback

        // then
        waitTillTaskTransitionsInto(State.UP, "0",2);
        executor.runAsync(() -> callbackEndpoint.succeed("0", "lol", ErrorOption.IGNORE));
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
    }

    private static void assertMilestoneTrigger(List<TransitionTimeDTO> triggerRecords, boolean withCallback, boolean endsWithFail) {
        var std = List.of(Transition.NEW_to_WAITING,
                Transition.WAITING_to_ENQUEUED,
                Transition.ENQUEUED_to_STARTING,
                Transition.STARTING_to_UP);

        List<Transition> body;
        if (withCallback){
            body = List.of(Transition.UP_to_ROLLBACK_TRIGGERED,
                    Transition.ROLLBACK_TRIGGERED_to_ROLLBACK_REQUESTED,
                    Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                    Transition.ROLLINGBACK_to_ROLLEDBACK,
                    Transition.ROLLEDBACK_to_NEW);
        } else {
            body = List.of(Transition.UP_to_ROLLBACK_TRIGGERED,
                    Transition.ROLLBACK_TRIGGERED_to_ROLLEDBACK,
                    Transition.ROLLEDBACK_to_NEW);
        }

        List<Transition> transitions = new ArrayList<>();
        transitions.addAll(std);
        transitions.addAll(body);
        transitions.addAll(std);

        if (endsWithFail) {
            transitions.add(Transition.UP_to_FAILED);
        }

        assertThat(triggerRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(transitions.toArray(new Transition[0]));
    }

    private static void assertRolledBackTask(List<TransitionTimeDTO> taskRecords, boolean withCallback, boolean isWaiting) {
        assertRolledBackTask(taskRecords, withCallback, isWaiting, false);
    }

    private static void assertRolledBackTask(List<TransitionTimeDTO> taskRecords, boolean withCallback, boolean isWaiting, boolean hasDependencies) {
        List<Transition> std;
        if (hasDependencies) {
            std = List.of(Transition.NEW_to_WAITING,
                    Transition.WAITING_to_ENQUEUED,
                    Transition.ENQUEUED_to_STARTING,
                    Transition.STARTING_to_UP,
                    Transition.UP_to_SUCCESSFUL);
        } else {
            std = List.of(Transition.NEW_to_ENQUEUED,
                    Transition.ENQUEUED_to_STARTING,
                    Transition.STARTING_to_UP,
                    Transition.UP_to_SUCCESSFUL);
        }

        List<Transition> body;
        if (withCallback) {
            if (isWaiting) {
                body = List.of(Transition.SUCCESSFUL_to_TO_ROLLBACK,
                        Transition.TO_ROLLBACK_to_ROLLBACK_REQUESTED,
                        Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                        Transition.ROLLINGBACK_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW);

            } else {
                body = List.of(Transition.SUCCESSFUL_to_ROLLBACK_REQUESTED,
                        Transition.ROLLBACK_REQUESTED_to_ROLLINGBACK,
                        Transition.ROLLINGBACK_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW);
            }
        } else {
            if (isWaiting) {
                body = List.of(Transition.SUCCESSFUL_to_TO_ROLLBACK,
                        Transition.TO_ROLLBACK_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW);
            } else {
                body = List.of(Transition.SUCCESSFUL_to_ROLLEDBACK,
                        Transition.ROLLEDBACK_to_NEW);
            }
        }

        List<Transition> transitions = new ArrayList<>();
        transitions.addAll(std);
        transitions.addAll(body);
        transitions.addAll(std);

        assertThat(taskRecords)
                .extracting(TransitionTimeDTO::getTransition)
                .containsExactly(transitions.toArray(new Transition[0]));
    }
}
