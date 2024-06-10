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
    UP_to_SUCCESSFUL(State.UP, State.SUCCESSFUL);

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
