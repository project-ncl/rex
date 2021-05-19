package org.jboss.pnc.scheduler.facade;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.api.TaskRegistry;
import org.jboss.pnc.scheduler.core.api.TaskTarget;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.facade.api.TaskProvider;
import org.jboss.pnc.scheduler.facade.mapper.GraphsMapper;
import org.jboss.pnc.scheduler.facade.mapper.TaskMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskProviderImpl implements TaskProvider {

    private TaskTarget target;

    private TaskRegistry registry;

    private TaskMapper mapper;

    private GraphsMapper graphMapper;

    private TaskController controller;

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
    public List<TaskDTO> getAll(boolean waiting, boolean running, boolean finished) {
        return registry.getTask(waiting,running,finished).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancel(String taskName) {
        controller.setMode(taskName, Mode.CANCEL);
    }

    @Override
    public TaskDTO get(String taskName) {
        return mapper.toDTO(registry.getTask(taskName));
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
