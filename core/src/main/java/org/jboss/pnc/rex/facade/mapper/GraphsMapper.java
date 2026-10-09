/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.mapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, uses = { EdgeMapper.class, CreateTaskMapper.class })
public interface GraphsMapper extends EntityMapper<CreateGraphRequest, TaskGraph> {

    @Override
    @Mapping(target = "edge", ignore = true)
    @Mapping(target = "queue", ignore = true)
    @Mapping(target = "correlationID", ignore = true)
    @Mapping(target = "graphConfiguration", ignore = true)
    CreateGraphRequest toDTO(TaskGraph dbEntity);

    @Override
    @Mapping(target = "edge", ignore = true)
    //correlationID is used in applyCorrelationID method
    //graphConfiguration is used in mergeWithGraphConfig method
    //queue is used in mergeWithGraphQueue method
    @BeanMapping(ignoreUnmappedSourceProperties = { "correlationID", "graphConfiguration", "queue" })
    TaskGraph toDB(CreateGraphRequest dtoEntity);

    @AfterMapping
    default void applyCorrelationID(CreateGraphRequest source, @MappingTarget TaskGraph.TaskGraphBuilder target) {
        if (source.correlationID != null && !source.correlationID.isBlank()) {
            var vertices = new HashMap<>(target.build().getVertices());
            for (var entry : vertices.entrySet()) {
                entry.setValue(
                        entry.getValue()
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

    @BeforeMapping
    default void mergeWithGraphQueue(CreateGraphRequest request) {
        if (request == null || request.getQueue() == null) {
            return;
        }

        for (var entry : request.getVertices().entrySet()) {
            var globalQueueSetting = request.getQueue();
            var taskQueueSetting = entry.getValue().getQueue();

            // apply queue configured in the top of the graph if task has it unspecified
            if (taskQueueSetting == null) {
                entry.getValue().queue = globalQueueSetting;
            }
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
                    graphConfig.cancelTimeout,
                    graphConfig.delayDependantsForFinalNotification,
                    graphConfig.rollbackLimit,
                    graphConfig.heartbeatEnable,
                    graphConfig.heartbeatInitialDelay,
                    graphConfig.heartbeatInterval,
                    graphConfig.heartbeatToleranceThreshold);
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
        Boolean delayDependantsForFinalNotification = taskConfig.delayDependantsForFinalNotification;
        if (taskConfig.delayDependantsForFinalNotification == null
                && graphConfig.delayDependantsForFinalNotification != null) {
            delayDependantsForFinalNotification = graphConfig.delayDependantsForFinalNotification;
        }
        Integer rollbackLimit = taskConfig.rollbackLimit;
        if (taskConfig.rollbackLimit == null && graphConfig.rollbackLimit != null) {
            rollbackLimit = graphConfig.rollbackLimit;
        }
        Boolean heartbeatEnable = taskConfig.heartbeatEnable;
        if (taskConfig.heartbeatEnable == null && graphConfig.heartbeatEnable != null) {
            heartbeatEnable = graphConfig.heartbeatEnable;
        }
        Duration heartbeatInitialDelay = taskConfig.heartbeatInitialDelay;
        if (taskConfig.heartbeatInitialDelay == null && graphConfig.heartbeatInitialDelay != null) {
            heartbeatInitialDelay = graphConfig.heartbeatInitialDelay;
        }
        Duration heartbeatInterval = taskConfig.heartbeatInterval;
        if (taskConfig.heartbeatInterval == null && graphConfig.heartbeatInterval != null) {
            heartbeatInterval = graphConfig.heartbeatInterval;
        }
        Integer heartbeatToleranceThreshold = taskConfig.heartbeatToleranceThreshold;
        if (taskConfig.heartbeatToleranceThreshold == null && graphConfig.heartbeatToleranceThreshold != null) {
            heartbeatToleranceThreshold = graphConfig.heartbeatToleranceThreshold;
        }

        return new ConfigurationDTO(
                passResultsOfDependencies,
                passMDCInRequestBody,
                passOTELInRequestBody,
                mdcHeaderKeys,
                cancelTimeout,
                delayDependantsForFinalNotification,
                rollbackLimit,
                heartbeatEnable,
                heartbeatInitialDelay,
                heartbeatInterval,
                heartbeatToleranceThreshold);
    }
}
