/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.enums;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum Origin {

    /**
     * The response originates from an external remote entity. This response signifies a callback for start/cancel
     * operations.
     */
    @ProtoEnumValue(number = 0) REMOTE_ENTITY,

    /**
     * A response for the transition originates in Rex itself. The generated response contains an error with the reason
     * for failure.
     *
     * An example of a failure can be failed invocation of remote entity whilst starting/cancelling.
     */
    @ProtoEnumValue(number = 1) REX_INTERNAL_ERROR,

    /**
     * The response originates out of Rex which was reached after timeout.
     */
    @ProtoEnumValue(number = 2) REX_TIMEOUT,

    /**
     * The response originates out of Rex, which was triggered after Task failed to comply with Heartbeat criteria
     */
    @ProtoEnumValue(number = 3) REX_HEARTBEAT_TIMEOUT
}
