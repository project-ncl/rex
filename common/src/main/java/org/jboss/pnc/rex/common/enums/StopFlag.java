/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * Flag which signifies a reason why the Task stopped execution.
 */
public enum StopFlag {
    /**
     * Default state.
     */
    @ProtoEnumValue(number = 0)
    NONE,

    /**
     * A Task was requested to be cancelled.
     */
    @ProtoEnumValue(number = 1)
    CANCELLED,

    /**
     * A Task has failed its execution remotely.
     */
    @ProtoEnumValue(number = 2)
    UNSUCCESSFUL,

    /**
     * A Task's dependency(can be transitive) has failed.
     */
    @ProtoEnumValue(number = 3)
    DEPENDENCY_FAILED,

    /**
     * A Task's dependency(can be transitive) has configured to wait for successful final notification, which has failed.
     *
     * NOTE: The dependency will be in state SUCCESS but the notification request received 4xx+ result.
     */
    @ProtoEnumValue(number = 4)
    DEPENDENCY_NOTIFY_FAILED
}
