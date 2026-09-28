/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.mapper;

import org.jboss.pnc.rex.facade.mapper.MapperCentralConfig;
import org.jboss.pnc.rex.facade.mapper.TransitionTimeMapper;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.MinimizedTask;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class, uses = {TransitionTimeMapper.class})
public interface MiniTaskMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"unfinishedDependencies", "starting", "controllerMode", "disposable",
            "rollbackMeta", "heartbeatMeta"})
    MinimizedTask minimize(Task task);
}
