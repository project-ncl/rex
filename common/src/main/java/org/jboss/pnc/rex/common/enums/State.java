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
 * The enum represents State the Task is currently in.
 * <p>
 * State represents vertices in the state-machine diagram.
 */
public enum State {

    /**
     * Task was created and is being idle. It does not transition unless Mode.ACTIVE.
     */
    @ProtoEnumValue(number = 0)
    NEW(StateGroup.IDLE),
    /**
     * Controller is waiting for either all dependencies to successfully complete.
     */
    @ProtoEnumValue(number = 1)
    WAITING(StateGroup.IDLE),
    /**
     * Task is queued and can be started at any time.
     */
    @ProtoEnumValue(number = 2)
    ENQUEUED(StateGroup.QUEUED),
    /**
     * Task is taken from a Queue and Controller makes a request to remote entity to start the Task remotely. Based on
     * the status code in response, Task will transition to either UP or START_FAILED.
     */
    @ProtoEnumValue(number = 3)
    STARTING(StateGroup.RUNNING),
    /**
     * Remote Task successfully started and is running.
     */
    @ProtoEnumValue(number = 4)
    UP(StateGroup.RUNNING),
    /**
     * Controller was requested to stop the remote Task and waits for a response from remote entity. Based on the
     * status code in response, Task will transition to either STOPPING or STOP_FAILED.
     */
    @ProtoEnumValue(number = 5)
    STOP_REQUESTED(StateGroup.RUNNING),
    /**
     * Remote Task started the cancellation process and is running.
     */
    @ProtoEnumValue(number = 6)
    STOPPING(StateGroup.RUNNING),
    /**
     * Task failed to start. Remote entity was either unreachable, didn't respond with 2xx status code or unexpected
     * error happened.
     */
    @ProtoEnumValue(number = 7)
    START_FAILED(StateGroup.FINAL),
    /**
     * Task failed to stop. Remote entity was either unreachable, didn't respond with 2xx status code or unexpected
     * error happened.
     */
    @ProtoEnumValue(number = 8)
    STOP_FAILED(StateGroup.FINAL),
    /**
     * Remote Task failed during execution.
     */
    @ProtoEnumValue(number = 9)
    FAILED(StateGroup.FINAL),
    /**
     * Remote Task ended successfully.
     */
    @ProtoEnumValue(number = 10)
    SUCCESSFUL(StateGroup.FINAL),
    /**
     * Remote Task stopped successfully.
     */
    @ProtoEnumValue(number = 11)
    STOPPED(StateGroup.FINAL),
    /**
     * Task triggered rollback. Controller will tell Milestone Task to start rollback process.
     */
    @ProtoEnumValue(number = 12)
    ROLLBACK_TRIGGERED(StateGroup.ROLLBACK_TODO),
    /**
     * Task was marked to rollback.
     */
    @ProtoEnumValue(number = 13)
    TO_ROLLBACK(StateGroup.ROLLBACK),
    /**
     * Task is in process of rolling back. Controller was requested to rollback the remote Task and waits for a response
     * from remote entity. The response (both positive+negative) will transition the Task into ROLLEDBACK state.
     *
     * (There's no ROLLBACK_FAILED state to not block the rollback process.)
     */
    @ProtoEnumValue(number = 14)
    ROLLBACK_REQUESTED(StateGroup.ROLLBACK),
    /**
     * Task is in process of rolling back. Controller is waiting for a callback.
     */
    @ProtoEnumValue(number = 15)
    ROLLINGBACK(StateGroup.ROLLBACK),
    /**
     * Task has rolledback (successfully or not). Soon it will restart its state and Transition back to IDLE or
     * QUEUED state.
     */
    @ProtoEnumValue(number = 16)
    ROLLEDBACK(StateGroup.ROLLBACK),
    /**
     * Task has failed to rollback. This does not affect the rollback process negatively.
     */
    @ProtoEnumValue(number = 17)
    ROLLBACK_FAILED(StateGroup.ROLLBACK);

    private final StateGroup type;

    State(StateGroup type) {
        this.type = type;
    }

    public StateGroup getGroup() {
        return type;
    }

    public boolean isIdle() {
        return type.equals(StateGroup.IDLE);
    }

    public boolean isRunning() {
        return type.equals(StateGroup.RUNNING);
    }

    public boolean isFinal() {
        return type.equals(StateGroup.FINAL);
    }

    public boolean isQueued() {
        return type.equals(StateGroup.QUEUED);
    }

    public boolean isAwaitingRollback() {
        return type.equals(StateGroup.ROLLBACK_TODO);
    }

    public boolean isRollback() {
        return type.equals(StateGroup.ROLLBACK);
    }
}
