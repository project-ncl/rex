package org.jboss.pnc.scheduler.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * The enum State Group.
 */
public enum StateGroup {
    /**
     * Controller is idle and job(Service) hasn't started remote execution.
     * <p>
     * In this state you are able to add additional dependencies.
     */
    IDLE,
    /**
     * Job(Service) is remotely active.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    RUNNING,
    /**
     * Job(Service) has finished execution or failed.
     * <p>
     * In this state you are unable to add additional dependencies and cannot transition to any other state.
     */
    FINAL
}
