package org.jboss.pnc.scheduler.rest;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.facade.api.TaskProvider;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;

import javax.inject.Inject;

public class InternalEndpointImpl implements InternalEndpoint {

    private TaskProvider taskProvider;

    //CDI
    @Deprecated
    public InternalEndpointImpl() {
    }

    @Inject
    public InternalEndpointImpl(TaskProvider provider) {
        this.taskProvider = provider;
    }

    @Override
    @Retry(maxRetries = 5)
    public void finish(String serviceName, FinishRequest result) {
        taskProvider.acceptRemoteResponse(ServiceName.parse(serviceName), result.getStatus());
    }
}
