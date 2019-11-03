package org.jboss.pnc.scheduler.core.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.pnc.scheduler.core.api.ServiceController;

@Builder
@Getter
@Setter
public class ServerResponse {
    private boolean positive;

    @ProtoFactory
    public ServerResponse(boolean positive) {
        this.positive = positive;
    }

    @ProtoField(number = 1, defaultValue = "true")
    public boolean getPositive() {
        return positive;
    }
}
