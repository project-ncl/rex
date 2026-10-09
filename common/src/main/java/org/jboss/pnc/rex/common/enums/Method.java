/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum Method {

    @ProtoEnumValue(0) GET,
    @ProtoEnumValue(1) POST,
    @ProtoEnumValue(2) PUT,
    @ProtoEnumValue(3) PATCH,
    @ProtoEnumValue(4) DELETE,
    @ProtoEnumValue(5) HEAD,
    @ProtoEnumValue(6) OPTIONS
}
