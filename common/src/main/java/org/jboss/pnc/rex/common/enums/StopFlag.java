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
 * Flag which signifies a reason why the Task stopped execution.
 */
public enum StopFlag {
    /**
     * Default state.
     */
    @ProtoEnumValue(number = 0) NONE,

    /**
     * A Task was requested to be cancelled.
     */
    @ProtoEnumValue(number = 1) CANCELLED,

    /**
     * A Task has failed its execution remotely.
     */
    @ProtoEnumValue(number = 2) UNSUCCESSFUL,

    /**
     * A Task's dependency(can be transitive) has failed.
     */
    @ProtoEnumValue(number = 3) DEPENDENCY_FAILED
}
