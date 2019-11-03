package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.jboss.pnc.scheduler.core.model.StopFlag;

import javax.enterprise.inject.Produces;

public class StopFlagEnumMarshaller implements EnumMarshaller<StopFlag> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.core.model.Service.StopFlag";
    }

    @Override
    public Class<? extends StopFlag> getJavaClass() {
        return StopFlag.class;
    }

    @Override
    public StopFlag decode(int enumValue) {
        switch (enumValue) {
            case 0: return StopFlag.NONE;
            case 1: return StopFlag.CANCELLED;
            case 2: return StopFlag.UNSUCCESSFUL;
            case 3: return StopFlag.DEPENDENCY_FAILED;
            default: return null;
        }
    }

    @Override
    public int encode(StopFlag stopFlag) {
        switch (stopFlag) {
            case NONE: return 0;
            case CANCELLED: return 1;
            case UNSUCCESSFUL: return 2;
            case DEPENDENCY_FAILED: return 3;
            default: throw new IllegalArgumentException("Unexpected StopFlag value: " + stopFlag);
        }
    }

    @Produces
    BaseMarshaller stopFlagMarshaller() {
        return new StopFlagEnumMarshaller();
    }
}
