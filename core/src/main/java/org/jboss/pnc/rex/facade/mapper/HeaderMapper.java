/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.model.Header;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface HeaderMapper extends EntityMapper<Request.Header, Header> {

    @Override
    Request.Header toDTO(Header dbEntity);

    @Override
    Header toDB(Request.Header dtoEntity);
}
