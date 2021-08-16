package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.dto.HttpRequest;
import org.jboss.pnc.rex.model.Request;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class, uses = {HeaderMapper.class})
public interface RequestMapper extends EntityMapper<HttpRequest, Request> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = "byteAttachment")
    HttpRequest toDTO(Request dbEntity);

    @Override
    Request toDB(HttpRequest dtoEntity);
}
