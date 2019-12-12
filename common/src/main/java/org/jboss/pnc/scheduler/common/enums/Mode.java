package org.jboss.pnc.scheduler.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * TaskControllers communicate with each other through Jobs. Modes serve as a way for users and other entities to affect
 * Controller's behaviour.
 */
public enum Mode {
    /**
     * Controller does not attempt to start and sits idly. This is initial Mode.
     */
    @ProtoEnumValue(number = 0)
    IDLE,
    /**
     * Controller is actively trying to start its execution.
     */
    @ProtoEnumValue(number = 1)
    ACTIVE,
    /**
     * Controller is told to cancel its Task's execution.
     */
    @ProtoEnumValue(number = 2)
    CANCEL
}
