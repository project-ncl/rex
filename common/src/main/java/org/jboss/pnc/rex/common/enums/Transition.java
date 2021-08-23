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
package org.jboss.pnc.rex.common.enums;

/**
 * This enum represent Transition between States
 * <p>
 * Transition is a process where there could be a number of Jobs that need to be executed before a transition is
 * completed. Another transition cannot be initiated until these tasks are completed.
 * <p>
 * F.e. Transition between UP and STOPPING State creates a Job for each dependant Task to stop and another Job to send a
 * request to remote entity to stop the Task's remote execution. After these Jobs are completed, Task can complete its
 * Transition to STOPPED/STOP_FAILED (based on remote entity's response).
 * <p>
 * Transition is an edge in state-machine diagram.
 */
public enum Transition {
    /**
     * A new Task is set to Mode.ACTIVE and has unfinished dependencies.
     */
    NEW_to_WAITING(State.NEW, State.WAITING),
    /**
     * A new Task is set to Mode.ACTIVE and has no unfinished dependencies.
     *
     * Controller places the Task into a queue.
     */
    NEW_to_ENQUEUED(State.NEW, State.ENQUEUED),
    /**
     * Task's dependencies have successfully finished.
     *
     * Controller places the Task into queue.
     */
    WAITING_to_ENQUEUED(State.WAITING, State.ENQUEUED),
    /**
     * Controller has found a room to start the Task.
     *
     * Controller requests remote entity to start execution of remote Task.
     */
    ENQUEUED_to_STARTING(State.ENQUEUED, State.STARTING),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller requests remote entity to stop execution of remote Task and informs dependants that it's stopping.
     */
    UP_to_STOPPING(State.UP,State.STOPPING),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller requests remote entity to stop execution of remote Task and informs dependants that it's stopping.
     */
    STARTING_to_STOPPING(State.STARTING, State.STOPPING),
    /**
     * Controller has received positive response that remote Task stopped its execution.
     */
    STOPPING_to_STOPPED(State.STOPPING, State.STOPPED),
    /**
     * Controller has received negative response and remote Task failed to stop(could be f.e. unavailable).
     *
     * Controller informs Task's dependants that the Task failed.
     */
    STOPPING_to_STOP_FAILED(State.STOPPING,State.STOP_FAILED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    NEW_to_STOPPED(State.NEW, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    WAITING_to_STOPPED(State.WAITING, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs Task's dependants that the Task stopped.
     */
    ENQUEUED_to_STOPPED(State.ENQUEUED, State.STOPPED),
    /**
     * Controller received positive response that remote Task has successfully started its execution.
     */
    STARTING_to_UP(State.STARTING, State.UP),
    /**
     * Controller received negative response that remote Task failed to start its execution.
     *
     * Controller informs Task's dependants that the Task failed.
     */
    STARTING_to_START_FAILED(State.STARTING, State.START_FAILED),
    /**
     * Controller received a callback that remote Task failed during its execution.
     *
     * Controller informs Task's dependants that the Task failed.
     */
    UP_to_FAILED(State.UP, State.FAILED),
    /**
     * Controller received a callback that remote Task has successfully completed its execution.
     *
     * Controller informs Task's dependants that it successfully finished.
     */
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
