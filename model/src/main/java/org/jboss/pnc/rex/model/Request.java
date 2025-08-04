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
import org.jboss.pnc.rex.common.enums.Method;

import java.io.IOException;
import java.util.List;

import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToByteArray;
import static org.jboss.pnc.rex.common.util.SerializationUtils.convertToObject;

@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@Jacksonized
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final String url;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private final Method method;

    @Getter(onMethod_ = {@ProtoField(number = 3)})
    private final List<Header> headers;

    @Getter
    private final Object attachment;

    @ProtoFactory
    public Request(String url, Method method, List<Header> headers, byte[] byteAttachment) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        Object attachment;
        try {
            attachment = convertToObject(byteAttachment);
        } catch (IOException exception) {
            log.error("Unexpected IO error during construction of Request.class object. " + this, exception);
            attachment = null;
        } catch (ClassNotFoundException exception) {
            log.error("Attachment byte array could not be casted into an existing class. " + this, exception);
            attachment = null;
        }
        this.attachment = attachment;
    }

    @JsonIgnore
    @ProtoField(number = 4, type = Type.BYTES)
    public byte[] getByteAttachment() {
        try {
            return convertToByteArray(attachment);
        } catch (IOException exception) {
            log.error("Unexpected IO error when serializing Request.class attachment. " + this, exception);
        }
        return null;
    }
}
