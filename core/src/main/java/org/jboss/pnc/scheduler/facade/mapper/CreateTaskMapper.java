package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.core.model.InitialTask;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {RequestMapper.class})
public interface CreateTaskMapper extends EntityMapper<CreateTaskDTO, InitialTask> {

    @Override
    CreateTaskDTO toDTO(InitialTask dbEntity);

    @Override
    InitialTask toDB(CreateTaskDTO dtoEntity);
}
