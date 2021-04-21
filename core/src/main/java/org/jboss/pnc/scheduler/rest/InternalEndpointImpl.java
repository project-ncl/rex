package org.jboss.pnc.scheduler.rest;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.dto.responses.LongResponse;
import org.jboss.pnc.scheduler.facade.api.OptionsProvider;
import org.jboss.pnc.scheduler.facade.api.TaskProvider;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    @Retry(maxRetries = 5)
    public void finish(String serviceName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(serviceName, result.getStatus());
    }

    @Override
    @Retry(maxRetries = 5)
    public void setConcurrent(Long amount) {
        optionsProvider.setConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrent() {
        return optionsProvider.getConcurrency();
    }
}
