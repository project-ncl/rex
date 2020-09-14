package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.dto.RemoteLinksDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface RemoteLinksMapper extends EntityMapper<RemoteLinksDTO, RemoteAPI> {

    @Override
    RemoteLinksDTO toDTO(RemoteAPI dbEntity);

    @Override
    RemoteAPI toDB(RemoteLinksDTO dtoEntity);
}
