package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.core.model.InitialTask;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class, uses = {RequestMapper.class})
public interface CreateTaskMapper extends EntityMapper<CreateTaskDTO, InitialTask> {

    @Override
    CreateTaskDTO toDTO(InitialTask dbEntity);

    @Override
    InitialTask toDB(CreateTaskDTO dtoEntity);
}
