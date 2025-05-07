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
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * This enum represent Transition between States
 * <p>
 * Transition is a process where there could be a number of Jobs that need to be executed before a transition is
 * completed. Another transition cannot be initiated until these tasks are completed.
 * <p>
 * F.e. Transition between UP and STOP_REQUESTED State creates a Job for each dependant Task to stop and another Job to send a
 * request to remote entity to stop the Task's remote execution. After these Jobs are completed, Task can complete its
 * Transition to STOPPED/STOP_FAILED (based on remote entity's response).
 * <p>
 * Transition is an edge in state-machine diagram.
 */
public enum Transition {

    /**
     * Controller has received message that the rollback process is complete and Task can restore its state.
     *
     * This is triggered recursively after the last Task in rollback tree has finished (This Task is always the
     * milestone).
      */
    @ProtoEnumValue(number = 42)
    ROLLEDBACK_to_NEW(State.ROLLEDBACK, State.NEW),
    /**
     * Controller has received message that the rollback process is complete and Task can restore its state.
     *
     * This is propagated recursively after the last Task in rollback tree has finished (This Task is always the
     * milestone).
     *
     * Difference from ROLLEDBACK_to_NEW is that the remote entity responded negatively.
     */
    @ProtoEnumValue(number = 43)
    ROLLBACK_FAILED_to_NEW(State.ROLLBACK_FAILED, State.NEW),

    /**
     * A new Task is set to Mode.ACTIVE and has unfinished dependencies.
     */
    @ProtoEnumValue(number = 0)
    NEW_to_WAITING(State.NEW, State.WAITING),
    /**
     * A new Task is set to Mode.ACTIVE and has no unfinished dependencies.
     *
     * Controller places the Task into a queue.
     */
    @ProtoEnumValue(number = 1)
    NEW_to_ENQUEUED(State.NEW, State.ENQUEUED),
    /**
     * Task's dependencies have successfully finished.
     *
     * Controller places the Task into queue.
     */
    @ProtoEnumValue(number = 2)
    WAITING_to_ENQUEUED(State.WAITING, State.ENQUEUED),
    /**
     * Controller has found a room to start the Task.
     *
     * Controller requests remote entity to start execution of remote Task.
     */
    @ProtoEnumValue(number = 3)
    ENQUEUED_to_STARTING(State.ENQUEUED, State.STARTING),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller requests remote entity to stop execution of remote Task and informs dependants that it's stopping.
     */
    @ProtoEnumValue(number = 4)
    UP_to_STOP_REQUESTED(State.UP, State.STOP_REQUESTED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller requests remote entity to stop execution of remote Task and informs dependants that it's stopping.
     */
    @ProtoEnumValue(number = 5)
    STARTING_to_STOP_REQUESTED(State.STARTING, State.STOP_REQUESTED),
    /**
     * Controller has received positive response that remote Task began process of stopping its execution.
     */
    @ProtoEnumValue(number = 6)
    STOP_REQUESTED_to_STOPPING(State.STOP_REQUESTED, State.STOPPING),
    /**
     * Controller has received negative response and remote Task failed to stop(could be f.e. unavailable).
     *
     * Controller informs Task's dependants that the Task failed to stop.
     */
    @ProtoEnumValue(number = 7)
    STOP_REQUESTED_to_STOP_FAILED(State.STOP_REQUESTED, State.STOP_FAILED),
    /**
     * Controller has received negative callback from remote entity and remote Task failed to stop.
     *
     * Controller informs Task's dependants that the Task failed to stop.
     */
    @ProtoEnumValue(number = 8)
    STOPPING_TO_STOP_FAILED(State.STOPPING, State.STOP_FAILED),

    /**
     * Controller received a callback that remote Task has successfully stopped its execution.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 9)
    STOPPING_TO_STOPPED(State.STOPPING, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 10)
    NEW_to_STOPPED(State.NEW, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 11)
    WAITING_to_STOPPED(State.WAITING, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 12)
    ENQUEUED_to_STOPPED(State.ENQUEUED, State.STOPPED),
    /**
     * Controller received positive response that remote Task has successfully started its execution.
     */
    @ProtoEnumValue(number = 13)
    STARTING_to_UP(State.STARTING, State.UP),
    /**
     * Controller received negative response that remote Task failed to start its execution.
     *
     * Controller informs Task's dependants that the Task failed.
     */
    @ProtoEnumValue(number = 14)
    STARTING_to_START_FAILED(State.STARTING, State.START_FAILED),
    /**
     * Controller received a callback that remote Task failed during its execution.
     *
     * Controller informs Task's dependants that the Task failed.
     */
    @ProtoEnumValue(number = 15)
    UP_to_FAILED(State.UP, State.FAILED),
    /**
     * Controller received a callback that remote Task has successfully completed its execution.
     *
     * Controller informs Task's dependants that it successfully finished.
     */
    @ProtoEnumValue(number = 16)
    UP_to_SUCCESSFUL(State.UP, State.SUCCESSFUL),

    /**
     * Controller received a callback that remote Task failed during its execution with a Milestone task set.
     *
     * This scenario triggers a rollback process and controller will initiate a rollback from the Milestone.
     */
    @ProtoEnumValue(number = 17)
    UP_to_ROLLBACK_TRIGGERED(State.UP, State.ROLLBACK_TRIGGERED),

    /**
     * Controller received negative response that remote Task failed to start its execution with a Milestone task set.
     *
     * This scenario triggers a rollback process and controller will initiate a rollback from the Milestone.
     */
    @ProtoEnumValue(number = 18)
    STARTING_to_ROLLBACK_TRIGGERED(State.STARTING, State.ROLLBACK_TRIGGERED),

    /**
     * This task can fast-forward in the rollback process and doesn't need to request rollback remotely.
     *
     * This is the task that triggered the rollback process from its Milestone.
     */
    @ProtoEnumValue(number = 19)
    ROLLBACK_TRIGGERED_to_ROLLEDBACK(State.ROLLBACK_TRIGGERED, State.ROLLEDBACK),
    /**
     * Task has a remote rollback endpoint set. Controller requests remote entity to rollback actions done by this task.
     *
     * This is the task that triggered the rollback process from its Milestone.
     */
    @ProtoEnumValue(number = 20)
    ROLLBACK_TRIGGERED_to_ROLLBACK_REQUESTED(State.ROLLBACK_TRIGGERED, State.ROLLBACK_REQUESTED),

    /**
     * RollbackManager marked this task to rollback. Since it has not done any remote actions (it's just NEW), it will
     * fast-forward it to ROLLEDBACK state.
     */
    @ProtoEnumValue(number = 21)
    NEW_to_ROLLEDBACK(State.NEW, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback. Since it has not done any remote actions (it's WAITING), it will
     * fast-forward it to ROLLEDBACK state.
     */
    @ProtoEnumValue(number = 22)
    WAITING_to_ROLLEDBACK(State.WAITING, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback. Since it has not done any remote actions (it's ENQUEUED), it will
     * fast-forward it to ROLLEDBACK state. Controller will poke queue in case new Tasks need to be scheduled.
     */
    @ProtoEnumValue(number = 23)
    ENQUEUED_to_ROLLEDBACK(State.ENQUEUED, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback. StopFlag.CANCELLED Tasks cannot be rolledback. Only
     * StopFlag.DEPENDENCY_FAILED or StopFlag.DEPENDENCY_NOTIFY_FAILED can, if the source Task of failure (the one that
     * actually failed) is also involved.
     */
    @ProtoEnumValue(number = 24)
    STOPPED_to_ROLLEDBACK(State.STOPPED, State.ROLLEDBACK),

    /**
     * RollbackManager marked this task to rollback and there are dependants which are not yet ROLLEDBACK.
     */
    @ProtoEnumValue(number = 25)
    SUCCESSFUL_to_TO_ROLLBACK(State.SUCCESSFUL, State.TO_ROLLBACK),

    /**
     * RollbackManager marked this task to rollback and Task has a rollback endpoint set. Controller requests remote
     * entity to rollback actions done by this task.
     */
    @ProtoEnumValue(number = 26)
    UP_to_ROLLBACK_REQUESTED(State.UP, State.ROLLBACK_REQUESTED),
    /**
     * RollbackManager marked this task to rollback and Task has a rollback endpoint set. Controller requests remote
     * entity to rollback actions done by this task.
     */
    @ProtoEnumValue(number = 27)
    STARTING_to_ROLLBACK_REQUESTED(State.STARTING, State.ROLLBACK_REQUESTED),
    /**
     * RollbackManager marked this task to rollback and Task has a rollback endpoint set. Controller requests remote
     * entity to rollback actions done by this task.
     */
    @ProtoEnumValue(number = 28)
    START_FAILED_to_ROLLBACK_REQUESTED(State.START_FAILED, State.ROLLBACK_REQUESTED),
    /**
     * RollbackManager marked this task to rollback and Task has a rollback endpoint set. Controller requests remote
     * entity to rollback actions done by this task.
     */
    @ProtoEnumValue(number = 29)
    FAILED_to_ROLLBACK_REQUESTED(State.FAILED, State.ROLLBACK_REQUESTED),
    /**
     * RollbackManager marked this task to rollback, Task has a rollback endpoint set and all dependants are ROLLEDBACK
     * (or Task has no dependants). Controller requests remote entity to rollback actions done by this task.
     */
    @ProtoEnumValue(number = 30)
    SUCCESSFUL_to_ROLLBACK_REQUESTED(State.SUCCESSFUL, State.ROLLBACK_REQUESTED),

    /**
     * RollbackManager marked this task to rollback and Task doesn't have a rollback endpoint set. If the task finishes
     * remotely, the callback will be ignored.
     */
    @ProtoEnumValue(number = 31)
    UP_to_ROLLEDBACK(State.UP, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback and Task doesn't have a rollback endpoint set.
     */
    @ProtoEnumValue(number = 32)
    STARTING_to_ROLLEDBACK(State.STARTING, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback and Task doesn't have a rollback endpoint set.
     */
    @ProtoEnumValue(number = 33)
    FAILED_to_ROLLEDBACK(State.FAILED, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback, Task doesn't have a rollback endpoint set and all dependants are
     * ROLLEDBACK.
     */
    @ProtoEnumValue(number = 34)
    SUCCESSFUL_to_ROLLEDBACK(State.SUCCESSFUL, State.ROLLEDBACK),
    /**
     * RollbackManager marked this task to rollback and Task doesn't have a rollback endpoint set.
     */
    @ProtoEnumValue(number = 35)
    START_FAILED_to_ROLLEDBACK(State.START_FAILED, State.ROLLEDBACK),

    /**
     * All dependants are ROLLEDBACK and rollback endpoint is defined. Controller requests remote entity to rollback
     * actions done by this task.
     */
    @ProtoEnumValue(number = 36)
    TO_ROLLBACK_to_ROLLBACK_REQUESTED(State.TO_ROLLBACK, State.ROLLBACK_REQUESTED),

    /**
     * Controller has received positive response that a Task has successfully started remote rolledback.
     */
    @ProtoEnumValue(number = 37)
    ROLLBACK_REQUESTED_to_ROLLINGBACK(State.ROLLBACK_REQUESTED, State.ROLLINGBACK),
    /**
     * Controller received a callback that remote Task has successfully completed rolledback.
     *
     * Controller will inform dependencies that this Task has rolledback.
     */
    @ProtoEnumValue(number = 38)
    ROLLINGBACK_to_ROLLEDBACK(State.ROLLINGBACK, State.ROLLEDBACK),
    /**
     * All dependants are ROLLEDBACK and no rollback endpoint is set. Controller will fast-forward this task to
     * ROLLEDBACK and send a message to dependencies that this Task has rolledback.
     */
    @ProtoEnumValue(number = 39)
    TO_ROLLBACK_to_ROLLEDBACK(State.TO_ROLLBACK, State.ROLLEDBACK),

    /**
     * Controller received a callback that remote Task has failed to rolledback remotely.
     *
     * Controller will inform dependencies that this Task has rolledback.
     */
    @ProtoEnumValue(number = 40)
    ROLLINGBACK_to_ROLLBACK_FAILED(State.ROLLINGBACK, State.ROLLBACK_FAILED),
    /**
     * Controller has received negative response that Task couldn't start to rollback remotely.
     *
     * Controller will inform dependencies that this Task has rolledback.
     */
    @ProtoEnumValue(number = 41)
    ROLLBACK_REQUESTED_to_ROLLBACK_FAILED(State.ROLLBACK_REQUESTED, State.ROLLBACK_FAILED),

    @ProtoEnumValue(number = 44)
    ROLLBACK_TRIGGERED_to_STOPPED(State.ROLLBACK_TRIGGERED, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 45)
    ROLLEDBACK_to_STOPPED(State.ROLLEDBACK, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 46)
    ROLLBACK_FAILED_to_STOPPED(State.ROLLBACK_FAILED, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 47)
    ROLLINGBACK_to_STOPPED(State.ROLLEDBACK, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 48)
    TO_ROLLBACK_to_STOPPED(State.TO_ROLLBACK, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    @ProtoEnumValue(number = 49)
    ROLLBACK_REQUESTED_to_STOPPED(State.ROLLBACK_REQUESTED, State.STOPPED);


    private final State before;

    public State getBefore() {
        return before;
    }

    public State getAfter() {
        return after;
    }

    private final State after;

    Transition(State before, State after) {
        this.before = before;
        this.after = after;
    }
}
