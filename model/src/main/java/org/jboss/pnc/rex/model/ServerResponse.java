/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.ResponseFlag;
import org.jboss.pnc.rex.common.enums.State;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToByteArray;
import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToObject;

@Builder
@AllArgsConstructor
@ToString
@Slf4j
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Getter(onMethod_ = {@ProtoField(number = 4, type = Type.ENUM)})
    private final Origin origin;

    @Getter(onMethod_ = {@ProtoField(number = 5, defaultValue = "0")})
    private final int rollbackCounter;

    @Getter(onMethod_ = {@ProtoField(number = 6)})
    private final Set<ResponseFlag> flags;

    @ProtoFactory
    public ServerResponse(State state,
                          boolean positive,
                          byte[] byteBody,
                          Origin origin,
                          int rollbackCounter,
                          Set<ResponseFlag> flags) {
        this.state = state;
        this.positive = positive;
        this.origin = origin;
        Object body;
        try {
            body = convertToObject(byteBody);
        } catch (IOException exception) {
            log.error("Unexpected IO error during construction of ServerResponse.class object. {}", this, exception);
            body = null;
        } catch (ClassNotFoundException exception) {
            log.error("Body byte array could not be casted into an existing class. {}", this, exception);
            body = null;
        }
        this.body = body;
        this.rollbackCounter = rollbackCounter;
        this.flags = flags;
    }

    @JsonIgnore
    public boolean isNegative() {
        return !positive;
    }

    @JsonIgnore
    @ProtoField(number = 3, type = Type.BYTES)
    public byte[] getByteBody() {
        try {
            return convertToByteArray(this.body);
        } catch (IOException exception) {
            log.error("Unexpected IO error when serializing ServerResponse.class body. {}", this, exception);
        }
        return null;
    }

}
