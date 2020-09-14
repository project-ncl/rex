package org.jboss.pnc.scheduler.rest;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateTaskRequest;
import org.jboss.pnc.scheduler.facade.api.TaskProvider;
import org.jboss.pnc.scheduler.rest.api.TaskEndpoint;
import org.jboss.pnc.scheduler.rest.parameters.TaskFilterParameters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

@ApplicationScoped
public class TaskEndpointImpl implements TaskEndpoint {

    private final TaskProvider taskProvider;

    @Inject
    public TaskEndpointImpl(TaskProvider taskProvider) {
        this.taskProvider = taskProvider;
    }

    @Override
    public List<TaskDTO> create(@NotNull CreateTaskRequest request) {
        return taskProvider.create(request.getTasks());
    }

    @Override
    public List<TaskDTO> getAll(TaskFilterParameters filterParameters) {
        Boolean allFiltersAreFalse = !filterParameters.getFinished() && !filterParameters.getRunning() && !filterParameters.getWaiting();

        //If query is empty return all services
        if (allFiltersAreFalse) {
            return taskProvider.getAll(true,true,true);
        }
        return taskProvider.getAll(filterParameters.getWaiting(), filterParameters.getRunning(), filterParameters.getFinished());
    }

    @Override
    public TaskDTO getSpecific(String taskID) {
        return taskProvider.get(taskID);
    }

    @Override
    @Retry(maxRetries = 5)
    public void cancel(String taskID) {
        taskProvider.cancel(taskID);
    }

 /*   @Override
    public List<TaskDTO> getGraph(String serviceName) {
        return serviceProvider.getAllRelated(serviceName);
    }*/
}
