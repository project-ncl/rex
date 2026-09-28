/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.mapper;

import org.jboss.pnc.rex.core.model.InitialTask;
import org.jboss.pnc.rex.facade.mapper.MapperCentralConfig;
import org.jboss.pnc.rex.model.HeartbeatMetadata;
import org.jboss.pnc.rex.model.RollbackMetadata;
import org.jboss.pnc.rex.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.TreeSet;

@Mapper(config = MapperCentralConfig.class, imports = {TreeSet.class, RollbackMetadata.class, HeartbeatMetadata.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface InitialTaskMapper {

    @Mapping(target = "serverResponses", ignore = true)
    @Mapping(target = "dependants", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "stoppedCause", ignore = true)
    // initial values
    @Mapping(target = "controllerMode", source = "controllerMode", defaultValue = "ACTIVE")
    @Mapping(target = "unfinishedDependencies", constant = "0")
    @Mapping(target = "stopFlag", constant = "NONE")
    @Mapping(target = "state", constant = "NEW")
    @Mapping(target = "starting", constant = "false")
    @Mapping(target = "disposable", constant = "false")
    @Mapping(target = "timestamps", expression = "java( new TreeSet() )")
    @Mapping(target = "rollbackMeta", expression = "java( RollbackMetadata.init() )")
    @Mapping(target = "heartbeatMeta", expression = "java( HeartbeatMetadata.init() )")
    // Singular additions
    @Mapping(target = "serverResponse", ignore = true)
    @Mapping(target = "dependant", ignore = true)
    @Mapping(target = "dependency", ignore = true)
    Task fromInitialTask(InitialTask initialTask);
}
