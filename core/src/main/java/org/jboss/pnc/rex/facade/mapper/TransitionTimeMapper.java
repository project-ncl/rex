/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.dto.TransitionTimeDTO;
import org.jboss.pnc.rex.model.TransitionTime;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface TransitionTimeMapper extends EntityMapper<TransitionTimeDTO, TransitionTime> {

    @Override
    TransitionTimeDTO toDTO(TransitionTime dbEntity);

    @Override
    TransitionTime toDB(TransitionTimeDTO dtoEntity);
}
