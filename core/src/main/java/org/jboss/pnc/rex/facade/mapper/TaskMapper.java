package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {RequestMapper.class, ServerResponseMapper.class})
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "serverResponses", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants", "starting",
            "controllerMode"})
    TaskDTO toDTO(Task dbEntity);

    @Override
    @Mapping(target = "controllerMode", ignore = true)
    @Mapping(target = "unfinishedDependencies", ignore = true)
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    @Mapping(target = "stopFlag", ignore = true)
    @Mapping(target = "starting", ignore = true)
    @BeanMapping(ignoreUnmappedSourceProperties = {"stopFlag"})
    Task toDB(TaskDTO dtoEntity);
}
