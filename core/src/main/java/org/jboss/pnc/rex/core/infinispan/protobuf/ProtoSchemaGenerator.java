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
package org.jboss.pnc.rex.core.infinispan.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.ResourceType;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.Configuration;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.NodeResource;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.RollbackMetadata;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;
import org.jboss.pnc.rex.model.ispn.adapter.DurationAdapter;
import org.jboss.pnc.rex.model.ispn.adapter.HashMapStringyAdapter;
import org.jboss.pnc.rex.model.ispn.adapter.KeyValueString;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated classes in includeClasses
 */
@AutoProtoSchemaBuilder(
        schemaPackageName = "rex_model",
        schemaFilePath = "META-INF/",
        schemaFileName = "RexModel.proto",
        includeClasses = {
                ServerResponse.class,
                Task.class,
                Header.class,
                Method.class,
                Mode.class,
                State.class,
                StopFlag.class,
                Origin.class,
                Request.class,
                Configuration.class,
                Transition.class,
                TransitionTime.class,
                KeyValueString.class,
                HashMapStringyAdapter.class,
                DurationAdapter.class,
                NodeResource.class,
                ClusteredJobReference.class,
                ResourceType.class,
                CJobOperation.class,
                RollbackMetadata.class
                })
interface ProtoSchemaGenerator extends GeneratedSchema {
}
