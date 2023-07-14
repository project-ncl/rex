/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

/**
 * Mapper that converts database entity to DTO and vice versa
 *
 * @param <DTO> DTO entity type
 * @param <DB> Database entity type
 */
public interface EntityMapper<DTO, DB> {
    /**
     * Converts database entity to DTO entity.
     *
     * @param dbEntity database entity
     * @return Converted DTO entity
     */
    DTO toDTO(DB dbEntity);

    /**
     * Converts DTO entity to database entity.
     *
     * @param dtoEntity DTO entity.
     * @return Converted database entity.
     */
    DB toDB(DTO dtoEntity);
}
