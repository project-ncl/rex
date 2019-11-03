package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.MessageMarshaller;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.ServiceController;
import org.jboss.pnc.scheduler.core.model.RemoteAPI;
import org.jboss.pnc.scheduler.core.model.ServerResponse;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.core.model.StopFlag;

import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class ServiceMarshaller implements MessageMarshaller<Service> {

    @Override
    public String getTypeName() {
        return "org.jboss.pnc.scheduler.core.model.Service";
    }

    @Override
    public Class<? extends Service> getJavaClass() {
        return Service.class;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Service service) throws IOException {
        writer.writeObject("name", service.getName(), ServiceName.class);
        writer.writeObject("remoteEndpoints", service.getRemoteEndpoints(), RemoteAPI.class);
        writer.writeEnum("mode", service.getControllerMode());
        writer.writeEnum("state", service.getState());
        writer.writeCollection("dependants", service.getDependants(), ServiceName.class);
        writer.writeInt("unfinishedDependencies", service.getUnfinishedDependencies());
        writer.writeString("payload", service.getPayload());
        writer.writeCollection("dependencies", service.getDependencies(), ServiceName.class);
        writer.writeEnum("stopFlag", service.getStopFlag());
        writer.writeObject("serverResponse", service.getServerResponse(), ServerResponse.class);
    }

    @Override
    public Service readFrom(ProtoStreamReader reader) throws IOException {
        ServiceName name = reader.readObject("name", ServiceName.class);
        RemoteAPI remoteEndpoints = reader.readObject("remoteEndpoints", RemoteAPI.class);
        ServiceController.Mode mode = reader.readEnum("mode", ServiceController.Mode.class);
        ServiceController.State state = reader.readEnum("state", ServiceController.State.class);
        Set<ServiceName> dependants = reader.readCollection("dependants", new HashSet<>(), ServiceName.class);
        Integer unfinishedDependencies = reader.readInt("unfinishedDependencies");
        String payload = reader.readString("payload");
        Set<ServiceName> dependencies = reader.readCollection("dependencies", new HashSet<>(), ServiceName.class);
        StopFlag stopFlag = reader.readEnum("stopFlag", StopFlag.class);
        ServerResponse serverResponse = reader.readObject("serverResponse", ServerResponse.class);
        return Service.builder()
                .name(name)
                .remoteEndpoints(remoteEndpoints)
                .controllerMode(mode)
                .state(state)
                .dependants(dependants)
                .unfinishedDependencies(unfinishedDependencies)
                .payload(payload)
                .dependencies(dependencies)
                .stopFlag(stopFlag)
                .serverResponse(serverResponse)
                .build();
    }

    @Produces
    MessageMarshaller serviceMarshaller() {
        return new ServiceMarshaller();
    }
}
