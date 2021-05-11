package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.dto.ServerResponseDTO;
import org.jboss.pnc.scheduler.model.ServerResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface ServerResponseMapper extends EntityMapper<ServerResponseDTO, ServerResponse>{

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"byteBody", "negative"})
    ServerResponseDTO toDTO(ServerResponse dbEntity);

    @Override
    ServerResponse toDB(ServerResponseDTO dtoEntity);
}
