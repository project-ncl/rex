package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.jboss.pnc.scheduler.core.api.ServiceController;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class StateEnumMarshaller implements EnumMarshaller<ServiceController.State> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.core.model.Service.State";
    }

    @Override
    public Class<? extends ServiceController.State> getJavaClass() {
        return ServiceController.State.class;
    }

    @Override
    public ServiceController.State decode(int enumValue) {
        switch (enumValue) {
            case 0: return ServiceController.State.NEW;
            case 1: return ServiceController.State.WAITING;
            case 2: return ServiceController.State.STARTING;
            case 3: return ServiceController.State.UP;
            case 4: return ServiceController.State.STOPPING;
            case 5: return ServiceController.State.START_FAILED;
            case 6: return ServiceController.State.STOP_FAILED;
            case 7: return ServiceController.State.FAILED;
            case 8: return ServiceController.State.SUCCESSFUL;
            case 9: return ServiceController.State.STOPPED;
            default: return null;
        }
    }

    @Override
    public int encode(ServiceController.State state) throws IllegalArgumentException {
        switch (state) {
            case NEW: return 0;
            case WAITING: return 1;
            case STARTING: return 2;
            case UP: return 3;
            case STOPPING: return 4;
            case START_FAILED: return 5;
            case STOP_FAILED: return 6;
            case FAILED: return 7;
            case SUCCESSFUL: return 8;
            case STOPPED: return 9;
            default: throw new IllegalArgumentException("Unexpected Controller state value: " + state);
        }
    }

    @Produces
    BaseMarshaller stateEnumMarshaller() {
        return new StateEnumMarshaller();
    }
}
