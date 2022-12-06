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

import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.HashMap;

@Mapper(config = MapperCentralConfig.class, uses = {EdgeMapper.class, CreateTaskMapper.class})
public interface GraphsMapper extends EntityMapper<CreateGraphRequest, TaskGraph> {

    @Override
    @Mapping(target = "edge", ignore = true)
    @Mapping(target = "correlationID", ignore = true)
    CreateGraphRequest toDTO(TaskGraph dbEntity);

    @Override
    @Mapping(target = "edge", ignore = true)
    //correlationID is used in applyCorrelationID method
    @BeanMapping(ignoreUnmappedSourceProperties = {"correlationID"})
    TaskGraph toDB(CreateGraphRequest dtoEntity);

    @AfterMapping
    default void applyCorrelationID(CreateGraphRequest source, @MappingTarget TaskGraph.TaskGraphBuilder target) {
        if (source.correlationID != null && !source.correlationID.isBlank()) {
            var vertices = new HashMap<>(target.build().getVertices());
            for (var entry : vertices.entrySet()) {
                entry.setValue(entry.getValue()
                        .toBuilder()
                        .correlationID(source.correlationID)
                        .build());
            }

            //apply changes
            target.vertices(vertices);
        }
    }
}
