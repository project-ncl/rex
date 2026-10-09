/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.infinispan.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.types.java.CommonTypes;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.ResourceType;
import org.jboss.pnc.rex.common.enums.ResponseFlag;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.Configuration;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.HeartbeatMetadata;
import org.jboss.pnc.rex.model.NodeResource;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.RollbackMetadata;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;
import org.jboss.pnc.rex.model.ispn.adapter.DurationAdapter;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated classes in includeClasses
 */
@ProtoSchema(
        schemaPackageName = "rex_model",
        schemaFilePath = "META-INF/",
        schemaFileName = "RexModel.proto",
        dependsOn = { CommonTypes.class },
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
                DurationAdapter.class,
                NodeResource.class,
                ClusteredJobReference.class,
                ResourceType.class,
                CJobOperation.class,
                RollbackMetadata.class,
                HeartbeatMetadata.class,
                ResponseFlag.class,
        },
        syntax = ProtoSyntax.PROTO3,
        allowNullFields = true)
interface ProtoSchemaGenerator extends GeneratedSchema {
}
