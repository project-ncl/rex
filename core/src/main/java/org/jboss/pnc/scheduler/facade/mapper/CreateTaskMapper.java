package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.core.model.InitialTask;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {RemoteLinksMapper.class})
public interface CreateTaskMapper extends EntityMapper<CreateTaskDTO, InitialTask> {

    @Override
    @Mapping(target = "remoteLinks", source = "remoteEndpoints")
    CreateTaskDTO toDTO(InitialTask dbEntity);

    @Override
    @Mapping(target = "remoteEndpoints", source = "remoteLinks")
    InitialTask toDB(CreateTaskDTO dtoEntity);
}
