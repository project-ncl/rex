package org.jboss.pnc.rex.rest;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.common.exceptions.CircularDependencyException;
import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;
import org.jboss.pnc.rex.rest.api.TaskEndpoint;
import org.jboss.pnc.rex.rest.parameters.TaskFilterParameters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.Set;

@ApplicationScoped
public class TaskEndpointImpl implements TaskEndpoint {

    private final TaskProvider taskProvider;

    @Inject
    public TaskEndpointImpl(TaskProvider taskProvider) {
        this.taskProvider = taskProvider;
    }

    @Override
    @Retry(maxRetries = 5,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    CircularDependencyException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public Set<TaskDTO> start(CreateGraphRequest request) {
        return taskProvider.create(request);
    }

    @Override
    public Set<TaskDTO> getAll(TaskFilterParameters filterParameters) {
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
    @Retry(maxRetries = 5,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public void cancel(String taskID) {
        taskProvider.cancel(taskID);
    }
}
