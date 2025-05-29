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
package org.jboss.pnc.rex.common.enums;


import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum Origin {

    /**
     * The response originates from an external remote entity. This response signifies a callback for start/cancel
     * operations.
     */
    @ProtoEnumValue(number = 0)
    REMOTE_ENTITY,

    /**
     * A response for the transition originates in Rex itself. The generated response contains an error with the reason
     * for failure.
     *
     * An example of a failure can be failed invocation of remote entity whilst starting/cancelling.
     */
    @ProtoEnumValue(number = 1)
    REX_INTERNAL_ERROR,

    /**
     * The response originates out of Rex which was reached after timeout.
     */
    @ProtoEnumValue(number = 2)
    REX_TIMEOUT,

    /**
     * The response originates out of Rex, which was triggered after Task failed to comply with Heartbeat criteria
     */
    @ProtoEnumValue(number = 3)
    REX_HEARTBEAT_TIMEOUT
}
