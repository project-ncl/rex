package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.jboss.pnc.scheduler.core.model.ServerResponse;

@AutoProtoSchemaBuilder(includeClasses = {ServerResponse.class}, schemaPackageName = "org.jboss.pnc.scheduler.core.model")
interface ServerResponseContextInitialixer extends SerializationContextInitializer {
}
