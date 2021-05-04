package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Mapper(config = MapperCentralConfig.class, uses = {RequestMapper.class})
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "serverResponses", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants", "starting",
            "controllerMode"})
    TaskDTO toDTO(Task dbEntity);

    @Override
    @Mapping(target = "controllerMode", ignore = true)
    @Mapping(target = "unfinishedDependencies", ignore = true)
    @Mapping(target = "serverResponses", ignore = true)
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    @Mapping(target = "stopFlag", ignore = true)
    @Mapping(target = "starting", ignore = true)
    @BeanMapping(ignoreUnmappedSourceProperties = {"stopFlag"})
    Task toDB(TaskDTO dtoEntity);
}
