/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.common.ConfigurationDefaults;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.model.Configuration;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, imports = {ConfigurationDefaults.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ConfigurationMapper extends EntityMapper<ConfigurationDTO, Configuration> {

    @Override
    ConfigurationDTO toDTO(Configuration dbEntity);

    @Mapping(target = "passResultsOfDependencies", defaultValue = "" + ConfigurationDefaults.passResultsOfDependencies)
    @Mapping(target = "passMDCInRequestBody", defaultValue = "" + ConfigurationDefaults.passMDCInRequestBody)
    @Mapping(target = "passOTELInRequestBody", defaultValue = "" + ConfigurationDefaults.passOTELInRequestBody)
    @Mapping(target = "cancelTimeout", defaultExpression = "java( ConfigurationDefaults.cancelTimeout )")
    @Mapping(target = "delayDependantsForFinalNotification",
            defaultValue = "" + ConfigurationDefaults.delayDependantsForFinalNotification)
    @Mapping(target = "rollbackLimit", defaultValue = "" + ConfigurationDefaults.rollbackLimit)
    @Mapping(target = "heartbeatInterval", defaultExpression = "java( ConfigurationDefaults.heartbeatInterval )")
    @Mapping(target = "heartbeatInitialDelay", defaultExpression = "java( ConfigurationDefaults.heartbeatInitialDelay )")
    @Named("std") //avoid ambiguity
    Configuration _toDB(ConfigurationDTO dtoEntity);


    /**
     * Creating empty ConfigurationDTO will cause Mapstruct to fill default ConfigurationDTO.* properties if null.
     */
    @Override
    default Configuration toDB(ConfigurationDTO dtoEntity) {
        if (dtoEntity == null) {
            return _toDB(new ConfigurationDTO());
        }

        return _toDB(dtoEntity);
    }
}
