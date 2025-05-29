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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import java.io.IOException;
import java.time.Instant;

import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToByteArray;
import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToObject;

@Builder(toBuilder = true)
@AllArgsConstructor
@ToString
@Slf4j
@Jacksonized
public class HeartbeatMetadata {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final Instant lastBeat;

    @Getter
    private final Object lastStatus;

    @ProtoFactory
    public HeartbeatMetadata(Instant lastBeat, byte[] statusBytes) {
        this.lastBeat = lastBeat;
        Object lastStatus;
        try {
            lastStatus = convertToObject(statusBytes);
        } catch (IOException e) {
            log.error("Unexpected IO error during construction of ServerResponse.class object. {}", this, e);
            lastStatus = null;
        } catch (ClassNotFoundException e) {
            log.error("LastStatus byte array could not be casted into an existing class. {}", this, e);
            lastStatus = null;
        }
        this.lastStatus = lastStatus;
    }

    @JsonIgnore
    @ProtoField(number = 2, type = Type.BYTES)
    public byte[] getStatusBytes() {
        try {
            return convertToByteArray(this.lastStatus);
        } catch (IOException exception) {
            log.error("Unexpected IO error when serializing ServerResponse.class body. {}", this, exception);
        }
        return null;
    }

    public static HeartbeatMetadata init() {
        return new HeartbeatMetadata(null, null);
    }
}
