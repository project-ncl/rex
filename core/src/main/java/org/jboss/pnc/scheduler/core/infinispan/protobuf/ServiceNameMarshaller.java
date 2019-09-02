package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.MessageMarshaller;
import org.jboss.msc.service.ServiceName;

import javax.enterprise.inject.Produces;
import java.io.IOException;

/**
 * Marshaller for the key in the cache
 */
public class ServiceNameMarshaller implements MessageMarshaller<ServiceName> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.model.ServiceName";
    }

    @Override
    public Class<? extends ServiceName> getJavaClass() {
        return ServiceName.class;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ServiceName serviceName) throws IOException {
        writer.writeString("serviceName", serviceName.getCanonicalName());
    }

    @Override
    public ServiceName readFrom(ProtoStreamReader reader) throws IOException {
        return ServiceName.parse(reader.readString("serviceName"));
    }

    @Produces
    MessageMarshaller serviceNameMarshaller() {
        return new ServiceNameMarshaller();
    }
}