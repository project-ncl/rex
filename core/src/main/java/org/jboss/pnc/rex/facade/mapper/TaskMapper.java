/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.Task;
import org.mapstruct.*;

@Mapper(
        config = MapperCentralConfig.class,
        uses = {
                RequestMapper.class,
                ServerResponseMapper.class,
                ConfigurationMapper.class,
                TransitionTimeMapper.class })
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

    @Override
    @BeanMapping(
            ignoreUnmappedSourceProperties = {
                    "unfinishedDependencies",
                    "serverResponses",
                    "starting",
                    "controllerMode",
                    "disposable",
                    "rollbackMeta",
                    "heartbeatMeta" })
    TaskDTO toDTO(Task dbEntity);

    @Override
    @Mapping(target = "controllerMode", ignore = true)
    @Mapping(target = "unfinishedDependencies", ignore = true)
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    @Mapping(target = "starting", ignore = true)
    @Mapping(target = "disposable", ignore = true)
    @Mapping(target = "rollbackMeta", ignore = true)
    @Mapping(target = "heartbeatMeta", ignore = true)
    //    @BeanMapping(ignoreUnmappedSourceProperties = {"stopFlag"})
    Task toDB(TaskDTO dtoEntity);
}
