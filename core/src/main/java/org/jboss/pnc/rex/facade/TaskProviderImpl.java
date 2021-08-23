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
package org.jboss.pnc.rex.facade;

import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.api.TaskTarget;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;
import org.jboss.pnc.rex.facade.mapper.GraphsMapper;
import org.jboss.pnc.rex.facade.mapper.TaskMapper;
import org.jboss.pnc.rex.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.NotFoundException;
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

    @Inject
    public TaskProviderImpl(TaskContainer container, TaskController controller, TaskMapper mapper, GraphsMapper graphMapper) {
        this.target = container;
        this.registry = container;
        this.controller = controller;
        this.mapper = mapper;
        this.graphMapper = graphMapper;
    }

    @Override
    @Transactional
    public Set<TaskDTO> create(CreateGraphRequest request) {
        return target.install(graphMapper.toDB(request))
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TaskDTO> getAll(boolean waiting, boolean running, boolean finished) {
        return registry.getTask(waiting,running,finished).stream()
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
            throw new NotFoundException("Task " + taskName + " was not found.");
        }
    }

    @Override
    public List<TaskDTO> getAllRelated(String taskName) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    @Transactional
    public void acceptRemoteResponse(String taskName, boolean positive, Object response) {
        if (positive) {
            controller.accept(taskName, response);
        } else {
            controller.fail(taskName, response);
        }
    }
}
