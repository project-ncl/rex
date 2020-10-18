package org.jboss.pnc.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.scheduler.common.enums.State;

@Builder
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class ServerResponse {

    @Getter(onMethod_ = {@ProtoField(number = 1, type = Type.ENUM)})
    private State state;
    @Getter(onMethod_ = {@ProtoField(number = 2, defaultValue = "true")})
    private boolean positive;

    public boolean isNegative() {
        return !positive;
    }
}
