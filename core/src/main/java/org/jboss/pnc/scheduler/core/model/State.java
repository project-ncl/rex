package org.jboss.pnc.scheduler.core.model;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * The enum represents State the job(Service) is currently in.
 * <p>
 * State represents vertices in state-machine diagram.
 */
public enum State {

    /**
     * Service was created and is being idle. It does not transition unless Mode.ACTIVE.
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
     * Controller requests remote job(Service) to start and waits for callback to approve that remote job successfully started.
     */
    @ProtoEnumValue(number = 2)
    STARTING(StateGroup.RUNNING),
    /**
     * Remote job(Service) successfully started and is running.
     */
    @ProtoEnumValue(number = 3)
    UP(StateGroup.RUNNING),
    /**
     * Controller requests remote job(Service) to stop and waits for a callback to approve that remote job successfully stopped.
     */
    @ProtoEnumValue(number = 4)
    STOPPING(StateGroup.RUNNING),
    /**
     * Received callback that remote job(Service) failed to start.
     */
    @ProtoEnumValue(number = 5)
    START_FAILED(StateGroup.FINAL),
    /**
     * Received callback that remote job(Service) failed to stop.
     */
    @ProtoEnumValue(number = 6)
    STOP_FAILED(StateGroup.FINAL),
    /**
     * Remote job(Service) failed during execution.
     */
    @ProtoEnumValue(number = 7)
    FAILED(StateGroup.FINAL),
    /**
     * Remote job(Service) ended successfully.
     */
    @ProtoEnumValue(number = 8)
    SUCCESSFUL(StateGroup.FINAL),
    /**
     * Remote job(Service) stopped successfully.
     */
    @ProtoEnumValue(number = 9)
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
}
