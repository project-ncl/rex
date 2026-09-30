/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.core.model.InitialTask;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {RequestMapper.class, ConfigurationMapper.class})
public interface CreateTaskMapper extends EntityMapper<CreateTaskDTO, InitialTask> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"correlationID"})
    CreateTaskDTO toDTO(InitialTask dbEntity);

    @Override
    //this is filled by GraphsMapper
    @Mapping(target = "correlationID", ignore = true)
    InitialTask toDB(CreateTaskDTO dtoEntity);
}
