package org.jboss.pnc.scheduler.facade;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskBuilder;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.api.TaskRegistry;
import org.jboss.pnc.scheduler.core.api.TaskTarget;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.jboss.pnc.scheduler.facade.api.TaskProvider;
import org.jboss.pnc.scheduler.facade.mapper.TaskMapper;
import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskProviderImpl implements TaskProvider {

    private TaskTarget target;

    private TaskRegistry registry;

    private TaskMapper mapper;

    private TaskController controller;

    @Inject
    public TaskProviderImpl(TaskContainer container, TaskController controller, TaskMapper mapper) {
        this.target = container;
        this.registry = container;
        this.controller = controller;
        this.mapper = mapper;
    }

    @Override
    public List<TaskDTO> create(List<TaskDTO> tasks) {
        BatchTaskInstaller batchTaskInstaller = target.addTasks();
        //fill dependents with contextualToDB and filter out existing tasks
        Collection<Task> dbTasks = mapper.contextualToDB(tasks)
                .stream()
                .filter(task -> registry.getTask(task.getName()) == null)
                .collect(Collectors.toSet());
        for (Task taskModel : dbTasks) {
            TaskBuilder builder = batchTaskInstaller.addTask(taskModel.getName())
                    .setPayload(taskModel.getPayload())
                    .setInitialMode(taskModel.getControllerMode())
                    .setRemoteEndpoints(taskModel.getRemoteEndpoints());
            taskModel.getDependencies().forEach(builder::requires);
            taskModel.getDependants().forEach(builder::isRequiredBy);
            builder.install();
        }
        batchTaskInstaller.commit();
        List<TaskDTO> toReturn = new ArrayList<>();
        for (TaskDTO taskDTO : tasks) {
            toReturn.add(mapper.toDTO(registry.getRequiredTask(taskDTO.getName())));
        }
        return toReturn;
    }

    @Override
    public List<TaskDTO> getAll(boolean waiting, boolean running, boolean finished) {
        return registry.getTask(waiting,running,finished).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancel(String serviceName) {
        controller.setMode(serviceName, Mode.CANCEL);
    }

    @Override
    public TaskDTO get(String serviceName) {
        return mapper.toDTO(registry.getTask(serviceName));
    }

    @Override
    public List<TaskDTO> getAllRelated(String serviceName) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    @Transactional
    public void acceptRemoteResponse(String serviceName, boolean positive) {
        if (positive) {
            controller.accept(serviceName);
        } else {
            controller.fail(serviceName);
        }
    }
}
