package org.jboss.pnc.scheduler.core.mapper;

import org.jboss.pnc.scheduler.facade.mapper.MapperCentralConfig;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.model.requests.MinimizedTask;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface MiniTaskMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "serverResponses", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants", "starting",
            "controllerMode"})
    MinimizedTask minimize(Task task);
}
