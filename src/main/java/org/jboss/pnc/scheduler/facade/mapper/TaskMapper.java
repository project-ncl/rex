package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Mapper(config = MapperCentralConfig.class, uses = {RemoteLinksMapper.class, TaskMapper.ServiceNameMapper.class})
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

    @Override
    @Mapping(target = "links", source = "remoteEndpoints")
    @Mapping(target = "mode", source = "controllerMode")
    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "serverResponses", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants"})
    TaskDTO toDTO(Task dbEntity);

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
    Task toDB(TaskDTO dtoEntity);

    default Collection<Task> contextualToDB(Collection<TaskDTO> dtoCollection){
        Map<String, TaskDTO> dtoMap = dtoCollection.stream().collect(Collectors.toMap(TaskDTO::getName, identity()));
        for (TaskDTO taskDTO : dtoCollection) {
            for (String dependency : taskDTO.getDependencies()) {
                TaskDTO task = dtoMap.get(dependency);
                if (task != null) task.getDependants().add(taskDTO.getName());
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
