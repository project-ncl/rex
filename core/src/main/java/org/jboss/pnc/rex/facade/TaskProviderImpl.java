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
package org.jboss.pnc.rex.facade;

import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.common.util.MDCUtils;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.api.TaskTarget;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;
import org.jboss.pnc.rex.facade.api.TaskProvider;
import org.jboss.pnc.rex.facade.mapper.GraphsMapper;
import org.jboss.pnc.rex.facade.mapper.TaskMapper;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.MDC;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskProviderImpl implements TaskProvider {

    private final TaskTarget target;

    private final TaskRegistry registry;

    private final TaskMapper mapper;

    private final GraphsMapper graphMapper;

    private final TaskController controller;

    private final HttpHeaders httpHeaders;

    @Inject
    public TaskProviderImpl(TaskContainer container,
                            TaskController controller,
                            TaskMapper mapper,
                            GraphsMapper graphMapper,
                            HttpHeaders httpHeaders) {
        this.target = container;
        this.registry = container;
        this.controller = controller;
        this.mapper = mapper;
        this.graphMapper = graphMapper;
        this.httpHeaders = httpHeaders;
    }

    @Override
    @Transactional
    public Set<TaskDTO> create(CreateGraphRequest request) {
        try {
            if (request.graphConfiguration != null && request.graphConfiguration.mdcHeaderKeyMapping != null) {
                MDCUtils.applyMDCsFromHeadersMM(request.graphConfiguration.mdcHeaderKeyMapping, httpHeaders.getRequestHeaders());
            }

            return target.install(graphMapper.toDB(request))
                    .stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toSet());
        } finally {
            MDC.clear();
        }
    }

    @Override
    public Set<TaskDTO> getAll(boolean waiting, boolean running, boolean finished, boolean rollingback, List<String> queueFilter) {
        return registry.getTasks(waiting, waiting, running, finished, rollingback, queueFilter).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void cancel(String taskName) {
        controller.setMode(taskName, Mode.CANCEL);
    }

    @Override
    public TaskDTO get(String taskName) {
        Task task;
        try {
            task = registry.getRequiredTask(taskName);
            return mapper.toDTO(task);
        } catch (TaskMissingException e) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e, e.getTaskName()))
                    .build());
        }
    }

    @Override
    public Set<TaskDTO> getByCorrelationID(String correlationID) {
        return registry.getTasksByCorrelationID(correlationID).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TaskDTO> getAllRelated(String taskName) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    @Transactional
    public void acceptRemoteResponse(String taskName, boolean positive, boolean rollback, Object response) {
        if (positive) {
            controller.accept(taskName, response, Origin.REMOTE_ENTITY, rollback);
        } else {
            controller.fail(taskName, response, Origin.REMOTE_ENTITY, rollback);
        }
    }
}
