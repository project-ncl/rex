/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * This enumeration represents all available types of clustered jobs. This enum is used to identify persisted
 * ClusteredJobReferences so that they can be instantiated again after Job Failover.
 */
public enum CJobOperation {
    @ProtoEnumValue(number = 0)
    CANCEL_TIMEOUT,
    @ProtoEnumValue(number = 1)
    HEARTBEAT_VERIFY
}
