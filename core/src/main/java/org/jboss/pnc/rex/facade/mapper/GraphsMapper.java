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
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Mapper(config = MapperCentralConfig.class, uses = {EdgeMapper.class, CreateTaskMapper.class})
public interface GraphsMapper extends EntityMapper<CreateGraphRequest, TaskGraph> {

    @Override
    @Mapping(target = "edge", ignore = true)
    @Mapping(target = "correlationID", ignore = true)
    @Mapping(target = "graphConfiguration", ignore = true)
    CreateGraphRequest toDTO(TaskGraph dbEntity);

    @Override
    @Mapping(target = "edge", ignore = true)
    //correlationID is used in applyCorrelationID method
    //graphConfiguration is used in mergeWithGraphConfig method
    @BeanMapping(ignoreUnmappedSourceProperties = {"correlationID", "graphConfiguration"})
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

    @BeforeMapping
    default void mergeWithGraphConfig(CreateGraphRequest request) {
        if (request == null || request.graphConfiguration == null) {
            return;
        }

        for (var entry : request.getVertices().entrySet()) {
            var taskConfig = entry.getValue().getConfiguration();
            var graphConfig = request.getGraphConfiguration();

            // apply merged configuration
            entry.getValue().configuration = merge(taskConfig, graphConfig);
        }
    }

    /**
     * Merge graph-level and task-level configuration into one. The task-level configuration has priority. A field will
     * be overridden only if the task-level field IS NULL.
     *
     * @param taskConfig task-level config
     * @param graphConfig graph-level config
     * @return merged configuration
     */
    private static ConfigurationDTO merge(ConfigurationDTO taskConfig, ConfigurationDTO graphConfig) {
        if (taskConfig == null) {
            return new ConfigurationDTO(
                    graphConfig.passResultsOfDependencies,
                    graphConfig.passMDCInRequestBody,
                    graphConfig.passOTELInRequestBody,
                    graphConfig.mdcHeaderKeyMapping,
                    graphConfig.cancelTimeout);
        }

        Boolean passResultsOfDependencies = taskConfig.passResultsOfDependencies;
        if (taskConfig.passResultsOfDependencies == null && graphConfig.passResultsOfDependencies != null) {
            passResultsOfDependencies = graphConfig.passResultsOfDependencies;
        }
        Boolean passMDCInRequestBody = taskConfig.passMDCInRequestBody;
        if (taskConfig.passMDCInRequestBody == null && graphConfig.passMDCInRequestBody != null) {
            passMDCInRequestBody = graphConfig.passMDCInRequestBody;
        }
        Boolean passOTELInRequestBody = taskConfig.passOTELInRequestBody;
        if (taskConfig.passOTELInRequestBody == null && graphConfig.passOTELInRequestBody != null) {
            passOTELInRequestBody = graphConfig.passOTELInRequestBody;
        }
        Map<String, String> mdcHeaderKeys = taskConfig.mdcHeaderKeyMapping;
        if (taskConfig.mdcHeaderKeyMapping == null && graphConfig.mdcHeaderKeyMapping != null) {
            mdcHeaderKeys = graphConfig.mdcHeaderKeyMapping;
        }
        Duration cancelTimeout = taskConfig.cancelTimeout;
        if (taskConfig.cancelTimeout == null && graphConfig.cancelTimeout != null) {
            cancelTimeout = graphConfig.cancelTimeout;
        }

        return new ConfigurationDTO(
                passResultsOfDependencies,
                passMDCInRequestBody,
                passOTELInRequestBody,
                mdcHeaderKeys,
                cancelTimeout);
    }
}
