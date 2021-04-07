package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.core.model.Edge;
import org.jboss.pnc.scheduler.dto.EdgeDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface EdgeMapper extends EntityMapper<EdgeDTO, Edge> {

    @Override
    EdgeDTO toDTO(Edge dbEntity);

    @Override
    Edge toDB(EdgeDTO dtoEntity);
}
