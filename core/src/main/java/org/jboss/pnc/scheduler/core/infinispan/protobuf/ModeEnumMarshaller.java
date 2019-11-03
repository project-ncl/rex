package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.jboss.pnc.scheduler.core.api.ServiceController;

import javax.enterprise.inject.Produces;


public class ModeEnumMarshaller implements EnumMarshaller<ServiceController.Mode> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.core.model.Service.Mode";
    }

    @Override
    public Class<? extends ServiceController.Mode> getJavaClass() {
        return ServiceController.Mode.class;
    }

    @Override
    public ServiceController.Mode decode(int enumValue) {
        switch (enumValue) {
            case 0: return ServiceController.Mode.IDLE;
            case 1: return ServiceController.Mode.ACTIVE;
            case 2: return ServiceController.Mode.CANCEL;
            default: return null;
        }
    }

    @Override
    public int encode(ServiceController.Mode mode) throws IllegalArgumentException {
        switch (mode) {
            case IDLE: return 0;
            case ACTIVE: return 1;
            case CANCEL: return 2;
            default: throw new IllegalArgumentException("Unexpected Controller mode value: " + mode);
        }
    }

    @Produces
    BaseMarshaller modeEnumMarshaller() {
        return new ModeEnumMarshaller();
    }
}
