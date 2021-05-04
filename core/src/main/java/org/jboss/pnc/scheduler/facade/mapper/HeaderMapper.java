package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.dto.HeaderDTO;
import org.jboss.pnc.scheduler.model.Header;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface HeaderMapper extends EntityMapper<HeaderDTO, Header> {

    @Override
    HeaderDTO toDTO(Header dbEntity);

    @Override
    Header toDB(HeaderDTO dtoEntity);
}
