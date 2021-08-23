/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

/**
 * Modes serve as a way for users and other entities to affect Controller's behaviour.
 */
public enum Mode {
    /**
     * Controller does not attempt to start the Task.
     */
    @ProtoEnumValue(number = 0)
    IDLE,
    /**
     * Controller will actively try to start Task's execution.
     */
    @ProtoEnumValue(number = 1)
    ACTIVE,
    /**
     * Controller is told to cancel Task's execution.
     */
    @ProtoEnumValue(number = 2)
    CANCEL
}
