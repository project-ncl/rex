package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated entities in basePackages
 */
@AutoProtoSchemaBuilder(basePackages = "org.jboss.pnc.scheduler.core.model", schemaPackageName = "org.jboss.pnc.scheduler.core.model")
interface ServerResponseContextInitialixer extends SerializationContextInitializer {
}
