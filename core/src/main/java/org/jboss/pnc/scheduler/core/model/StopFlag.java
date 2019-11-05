package org.jboss.pnc.scheduler.core.model;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum StopFlag {
    @ProtoEnumValue(number = 0)
    NONE,

    @ProtoEnumValue(number = 1)
    CANCELLED,

    @ProtoEnumValue(number = 2)
    UNSUCCESSFUL,

    @ProtoEnumValue(number = 3)
    DEPENDENCY_FAILED
}
