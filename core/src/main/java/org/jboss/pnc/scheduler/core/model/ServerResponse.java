package org.jboss.pnc.scheduler.core.model;

import lombok.Builder;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.scheduler.common.enums.State;

@Builder
public class ServerResponse {
    private State state;

    private boolean positive;

    @ProtoFactory
    public ServerResponse(State state, boolean positive) {
        this.state = state;
        this.positive = positive;
    }

    @ProtoField(number = 1, type = Type.ENUM)
    public State getState() {
        return state;
    }

    @ProtoField(number = 2, defaultValue = "true")
    public boolean isPositive() {
        return positive;
    }

    public boolean isNegative() {
        return !positive;
    }
}
