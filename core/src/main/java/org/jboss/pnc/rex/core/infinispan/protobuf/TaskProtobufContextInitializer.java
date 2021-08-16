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
