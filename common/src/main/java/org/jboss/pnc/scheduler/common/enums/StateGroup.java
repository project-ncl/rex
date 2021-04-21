package org.jboss.pnc.scheduler.common.enums;

/**
 * The enum State Group.
 */
public enum StateGroup {
    /**
     * Controller is idle and task hasn't started remote execution.
     * <p>
     * In this state you are able to add additional dependencies.
     */
    IDLE,
    /**
     * Task is waiting in queue and can be started at any time. It's a state between being idle and running.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    QUEUED,
    /**
     * Task is remotely active.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    RUNNING,
    /**
     * Task has finished execution or failed.
     * <p>
     * In this state you are unable to add additional dependencies and cannot transition to any other state.
     */
    FINAL
}
