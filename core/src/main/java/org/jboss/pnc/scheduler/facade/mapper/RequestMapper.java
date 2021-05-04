package org.jboss.pnc.scheduler.facade.mapper;

import org.jboss.pnc.scheduler.dto.HttpRequest;
import org.jboss.pnc.scheduler.model.Request;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {HeaderMapper.class})
public interface RequestMapper extends EntityMapper<HttpRequest, Request> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = "byteAttachment")
    HttpRequest toDTO(Request dbEntity);

    @Override
    Request toDB(HttpRequest dtoEntity);
}
