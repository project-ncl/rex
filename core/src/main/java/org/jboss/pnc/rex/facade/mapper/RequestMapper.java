/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.model.Request;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperCentralConfig.class, uses = {HeaderMapper.class, UriMapper.class})
public interface RequestMapper extends EntityMapper<org.jboss.pnc.api.dto.Request, Request> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = "byteAttachment")
    @Mapping(target = "uri", source = "url")
    @Mapping(target = "header", ignore = true)
    @Mapping(target = "authTokenHeader", ignore = true)
    org.jboss.pnc.api.dto.Request toDTO(Request dbEntity);

    @Override
    @Mapping(target = "url", source = "uri")
    Request toDB(org.jboss.pnc.api.dto.Request dtoEntity);
}
