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
package org.jboss.pnc.rex.core.infinispan.protobuf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated classes in includeClasses
 */
@AutoProtoSchemaBuilder(schemaPackageName = "org.jboss.pnc.rex.model",
        includeClasses = {ServerResponse.class,
                Task.class,
                Header.class,
                Method.class,
                Mode.class,
                State.class,
                StopFlag.class,
                Request.class})
interface TaskProtobufContextInitializer extends SerializationContextInitializer {
}
