package org.jboss.pnc.rex.core.mapper;

import org.jboss.pnc.rex.core.model.InitialTask;
import org.jboss.pnc.rex.facade.mapper.MapperCentralConfig;
import org.jboss.pnc.rex.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class)
public interface InitialTaskMapper {

    @Mapping(target = "serverResponses", ignore = true)
    @Mapping(target = "dependants", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    // initial values
    @Mapping(target = "controllerMode", source = "controllerMode", defaultValue = "ACTIVE")
    @Mapping(target = "unfinishedDependencies", constant = "0")
    @Mapping(target = "stopFlag", constant = "NONE")
    @Mapping(target = "state", constant = "NEW")
    @Mapping(target = "starting", constant = "false")
    // Singular additions
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    Task fromInitialTask(InitialTask initialTask);
}
