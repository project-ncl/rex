package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.FileDescriptorSource;

import javax.enterprise.inject.Produces;

public class ServiceNameProtoFileProducer {

    @Produces
    FileDescriptorSource serviceNameProtoDefinition() {
        return FileDescriptorSource.fromString("service_name.proto", "package org.jboss.pnc.scheduler.model;\n" +
                "\n" +
                "message ServiceName {\n" +
                "  required string serviceName = 1;\n" +
                "}");
    }
}
