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

/**
 * Flags that can be set by remote entity on callback which slightly change the behaviour of Task Controller.
 */
public enum ResponseFlag {
    /**
     * If set, the controller will not trigger rollback from a Milestone even if the Task could.
     *
     * APPLICABLE only to negative callbacks. The flag doesn't do anything for positive callback.
     */
    @ProtoEnumValue(0)
    SKIP_ROLLBACK
}
