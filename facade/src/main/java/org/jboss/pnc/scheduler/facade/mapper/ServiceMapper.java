package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

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
    @Mapping(target = "stopFlag", ignore = true)
    @BeanMapping(ignoreUnmappedSourceProperties = {"stopFlag"})
    Service toDB(ServiceDTO dtoEntity);

    default Collection<Service> contextualToDB(Collection<ServiceDTO> dtoCollection){
        Map<String, ServiceDTO> dtoMap = dtoCollection.stream().collect(Collectors.toMap(ServiceDTO::getName, identity()));
        for (ServiceDTO serviceDTO : dtoCollection) {
            for (String dependency : serviceDTO.getDependencies()) {
                ServiceDTO service = dtoMap.get(dependency);
                if (service != null) service.getDependants().add(serviceDTO.getName());
            }
        }
        return dtoMap.values().stream().map(this::toDB).collect(Collectors.toSet());
    }

    class ServiceNameMapper {
        public static ServiceName toServiceName(String string) {
            return ServiceName.parse(string);
        }
        public static String fromServiceName(ServiceName serviceName) {
            return serviceName.getCanonicalName();
        }
    }
}
