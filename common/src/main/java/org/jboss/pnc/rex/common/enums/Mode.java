package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * Modes serve as a way for users and other entities to affect Controller's behaviour.
 */
public enum Mode {
    /**
     * Controller does not attempt to start the Task.
     */
    @ProtoEnumValue(number = 0)
    IDLE,
    /**
     * Controller will actively try to start Task's execution.
     */
    @ProtoEnumValue(number = 1)
    ACTIVE,
    /**
     * Controller is told to cancel Task's execution.
     */
    @ProtoEnumValue(number = 2)
    CANCEL
}
