package org.jboss.pnc.scheduler.common.enums;

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
     * Controller is waiting for either all dependencies to successfully complete
     * or for room in the Container(limited number of active tasks).
     */
    @ProtoEnumValue(number = 1)
    WAITING(StateGroup.IDLE),
    /**
     * Task is queued and can be started at any time.
     */
    @ProtoEnumValue(number = 2)
    ENQUEUED(StateGroup.QUEUED),
    /**
     * Controller requests remote Task to start and waits for callback to approve that remote job successfully started.
     */
    @ProtoEnumValue(number = 3)
    STARTING(StateGroup.RUNNING),
    /**
     * Remote Task successfully started and is running.
     */
    @ProtoEnumValue(number = 4)
    UP(StateGroup.RUNNING),
    /**
     * Controller requests remote Task to stop and waits for a callback to approve that remote job successfully stopped.
     */
    @ProtoEnumValue(number = 5)
    STOPPING(StateGroup.RUNNING),
    /**
     * Received callback that remote Task failed to start.
     */
    @ProtoEnumValue(number = 6)
    START_FAILED(StateGroup.FINAL),
    /**
     * Received callback that remote Task failed to stop.
     */
    @ProtoEnumValue(number = 7)
    STOP_FAILED(StateGroup.FINAL),
    /**
     * Remote Task failed during execution.
     */
    @ProtoEnumValue(number = 8)
    FAILED(StateGroup.FINAL),
    /**
     * Remote Task ended successfully.
     */
    @ProtoEnumValue(number = 9)
    SUCCESSFUL(StateGroup.FINAL),
    /**
     * Remote Task stopped successfully.
     */
    @ProtoEnumValue(number = 10)
    STOPPED(StateGroup.FINAL);

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
}
