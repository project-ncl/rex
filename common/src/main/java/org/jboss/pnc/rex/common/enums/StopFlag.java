package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * Flag which signifies a reason why the Task stopped execution.
 */
public enum StopFlag {
    /**
     * Default state.
     */
    @ProtoEnumValue(number = 0)
    NONE,

    /**
     * A Task was requested to be cancelled.
     */
    @ProtoEnumValue(number = 1)
    CANCELLED,

    /**
     * A Task has failed its execution remotely.
     */
    @ProtoEnumValue(number = 2)
    UNSUCCESSFUL,

    /**
     * A Task's dependency(can be transitive) has failed.
     */
    @ProtoEnumValue(number = 3)
    DEPENDENCY_FAILED
}
