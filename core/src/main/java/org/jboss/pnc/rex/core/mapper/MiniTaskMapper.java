package org.jboss.pnc.rex.core.mapper;

import org.jboss.pnc.rex.facade.mapper.MapperCentralConfig;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.MinimizedTask;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface MiniTaskMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "dependant",
            "dependency", "serverResponse", "stringName", "stringDependencies", "stringDependants", "starting",
            "controllerMode"})
    MinimizedTask minimize(Task task);
}
