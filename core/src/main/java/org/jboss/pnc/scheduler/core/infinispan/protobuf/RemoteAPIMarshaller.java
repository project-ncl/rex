package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.MessageMarshaller;
import org.jboss.pnc.scheduler.core.model.RemoteAPI;

import javax.enterprise.inject.Produces;
import java.io.IOException;

public class RemoteAPIMarshaller implements MessageMarshaller<RemoteAPI> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.core.model.RemoteAPI";
    }

    @Override
    public Class<? extends RemoteAPI> getJavaClass() {
        return RemoteAPI.class;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, RemoteAPI remoteAPI) throws IOException {
        writer.writeString("startUrl", remoteAPI.getStartUrl());
        writer.writeString("stopUrl", remoteAPI.getStopUrl());
    }

    @Override
    public RemoteAPI readFrom(ProtoStreamReader reader) throws IOException {
        String startUrl = reader.readString("startUrl");
        String stopUrl = reader.readString("stopUrl");
        return RemoteAPI.builder()
                .startUrl(startUrl)
                .stopUrl(stopUrl)
                .build();
    }

    @Produces
    MessageMarshaller remoteAPIMarshaller() {
        return new RemoteAPIMarshaller();
    }
}
