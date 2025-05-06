/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.facade.mapper;

import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.model.Configuration;
import org.mapstruct.*;

import static org.jboss.pnc.rex.model.Configuration.*;

@Mapper(config = MapperCentralConfig.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ConfigurationMapper extends EntityMapper<ConfigurationDTO, Configuration> {

    @Override
    ConfigurationDTO toDTO(Configuration dbEntity);

    @Mapping(target = "passResultsOfDependencies", defaultValue = "" + Defaults.passResultsOfDependencies)
    @Mapping(target = "passMDCInRequestBody", defaultValue = "" + Defaults.passMDCInRequestBody)
    @Mapping(target = "passOTELInRequestBody", defaultValue = "" + Defaults.passOTELInRequestBody)
    @Mapping(target = "cancelTimeout", defaultValue = Defaults.cancelTimeoutString)
    @Mapping(target = "delayDependantsForFinalNotification",
            defaultValue = "" + Defaults.delayDependantsForFinalNotification)
    @Mapping(target = "rollbackLimit", defaultValue = "" + Defaults.rollbackLimit)
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
