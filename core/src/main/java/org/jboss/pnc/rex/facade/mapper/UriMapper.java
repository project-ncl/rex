/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;

import java.net.URI;

@Mapper(config = MapperCentralConfig.class)
public class UriMapper implements EntityMapper<URI, String> {
    @Override
    public URI toDTO(String dbEntity) {
        return URI.create(dbEntity);
    }

    @Override
    public String toDB(URI dtoEntity) {
        return dtoEntity.toString();
    }
}
