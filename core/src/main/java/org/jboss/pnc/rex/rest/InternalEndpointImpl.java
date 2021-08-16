package org.jboss.pnc.rex.rest;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;
import org.jboss.pnc.rex.facade.api.TaskProvider;
import org.jboss.pnc.rex.rest.api.InternalEndpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

@ApplicationScoped
public class InternalEndpointImpl implements InternalEndpoint {

    private final TaskProvider taskProvider;

    private final OptionsProvider optionsProvider;

    @Inject
    public InternalEndpointImpl(TaskProvider provider, OptionsProvider optionsProvider) {
        this.taskProvider = provider;
        this.optionsProvider = optionsProvider;
    }

    @Override
    @Retry(maxRetries = 15,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public void finish(String taskName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(taskName, result.getStatus(), result.getResponse());
    }

    @Override
    @Retry(maxRetries = 5,
            delay = 10,
            jitter = 50,
            abortOn = {ConstraintViolationException.class,
                    TaskMissingException.class,
                    BadRequestException.class,
                    TaskConflictException.class})
    public void setConcurrent(Long amount) {
        optionsProvider.setConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return optionsProvider.getConcurrency();
    }
}
