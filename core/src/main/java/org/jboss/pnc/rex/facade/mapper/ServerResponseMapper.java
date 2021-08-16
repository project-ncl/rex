package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.model.ServerResponse;
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
