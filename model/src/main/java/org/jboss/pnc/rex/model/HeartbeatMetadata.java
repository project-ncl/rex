/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model;

import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToByteArray;
import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToObject;

import java.io.IOException;
import java.time.Instant;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@AllArgsConstructor
@ToString
@Slf4j
@Jacksonized
public class HeartbeatMetadata {

    @Getter(onMethod_ = { @ProtoField(number = 1) })
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
