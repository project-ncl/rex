/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.pnc.rex.common.enums.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@ToString
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor_ = { @ProtoFactory })
public class NodeResource {
    @Getter(onMethod_ = { @ProtoField(number = 1) })
    private final String ownerNode;

    @Getter(onMethod_ = { @ProtoField(number = 2) })
    private final String resourceId;

    @Getter(onMethod_ = { @ProtoField(number = 3) })
    private final ResourceType resourceType;
}
