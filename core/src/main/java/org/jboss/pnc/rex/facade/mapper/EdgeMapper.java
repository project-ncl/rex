/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.core.model.Edge;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface EdgeMapper extends EntityMapper<EdgeDTO, Edge> {

    @Override
    EdgeDTO toDTO(Edge dbEntity);

    @Override
    Edge toDB(EdgeDTO dtoEntity);
}
