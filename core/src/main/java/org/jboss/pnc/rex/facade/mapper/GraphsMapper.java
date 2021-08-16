package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {EdgeMapper.class, CreateTaskMapper.class})
public interface GraphsMapper extends EntityMapper<CreateGraphRequest, TaskGraph> {

    @Override
    @Mapping(target = "edge", ignore = true)
    //@Mapping(target = "vertex", ignore = true)
    CreateGraphRequest toDTO(TaskGraph dbEntity);

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"edge"/*, "vertex"*/})
    TaskGraph toDB(CreateGraphRequest dtoEntity);
}
