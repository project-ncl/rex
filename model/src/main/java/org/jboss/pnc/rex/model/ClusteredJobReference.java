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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.rex.common.enums.CJobOperation;

import java.util.HashMap;
import java.util.Map;

@ToString
@Jacksonized
@EqualsAndHashCode
@Builder(toBuilder = true)
@Indexed
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class ClusteredJobReference {
    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final String id;

    @Getter(onMethod_ = {@ProtoField(number = 2), @Basic})
    private final String owner;

    @Getter(onMethod_ = {@ProtoField(number = 3), @Basic})
    private final CJobOperation type;

    @Getter(onMethod_ = {@ProtoField(number = 4, mapImplementation = HashMap.class)})
    private final Map<String,String> telemetry;

    @Getter(onMethod_ = {@ProtoField(number = 5), @Basic})
    private final String taskName;

    public boolean isOwnedBy(String instance) {
        return owner.equals(instance);
    }
}
