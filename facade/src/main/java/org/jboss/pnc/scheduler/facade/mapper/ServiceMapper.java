package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {RemoteLinksMapper.class, ServiceMapper.ServiceNameMapper.class})
public interface ServiceMapper extends EntityMapper<ServiceDTO, Service> {

    @Override
    @Mapping(target = "links", source = "remoteEndpoints")
    @Mapping(target = "mode", source = "controllerMode")
    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "serverResponses", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants"})
    ServiceDTO toDTO(Service dbEntity);

    @Override
    @Mapping(target = "controllerMode", source = "mode")
    @Mapping(target = "remoteEndpoints", source = "links")
    @Mapping(target = "unfinishedDependencies", ignore = true)
    @Mapping(target = "serverResponses", ignore = true)
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    Service toDB(ServiceDTO dtoEntity);

    class ServiceNameMapper {
        public static ServiceName toServiceName(String string) {
            return ServiceName.parse(string);
        }
        public static String fromServiceName(ServiceName serviceName) {
            return serviceName.getCanonicalName();
        }
    }
}
