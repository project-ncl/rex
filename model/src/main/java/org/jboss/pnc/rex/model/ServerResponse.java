package org.jboss.pnc.rex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.rex.common.enums.State;

import java.io.IOException;

import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToByteArray;
import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToObject;

@Builder
@AllArgsConstructor
@Slf4j
@Jacksonized
public class ServerResponse {

    /**
     * Task's state when the Response from remote entity arrived (before transition)
     */
    @Getter(onMethod_ = {@ProtoField(number = 1, type = Type.ENUM)})
    private final State state;

    @Getter(onMethod_ = {@ProtoField(number = 2, defaultValue = "true")})
    private final boolean positive;

    @Getter
    private final Object body;

    @ProtoFactory
    public ServerResponse(State state, boolean positive, byte[] byteBody) {
        this.state = state;
        this.positive = positive;
        Object body;
        try {
            body = convertToObject(byteBody);
        } catch (IOException exception) {
            log.error("Unexpected IO error during construction of ServerResponse.class object. " + this, exception);
            body = null;
        } catch (ClassNotFoundException exception) {
            log.error("Body byte array could not be casted into an existing class. " + this, exception);
            body = null;
        }
        this.body = body;
    }

    public boolean isNegative() {
        return !positive;
    }

    @ProtoField(number = 3, type = Type.BYTES)
    public byte[] getByteBody() {
        try {
            return convertToByteArray(this.body);
        } catch (IOException exception) {
            log.error("Unexpected IO error when serializing ServerResponse.class body. " + this, exception);
        }
        return null;
    }



}
