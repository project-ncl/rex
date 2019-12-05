package org.jboss.pnc.scheduler.rest;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.facade.api.ServiceProvider;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;

import javax.inject.Inject;

public class InternalEndpointImpl implements InternalEndpoint {

    private ServiceProvider serviceProvider;

    //CDI
    @Deprecated
    public InternalEndpointImpl() {
    }

    @Inject
    public InternalEndpointImpl(ServiceProvider provider) {
        this.serviceProvider = provider;
    }

    @Override
    public void finish(String serviceName, FinishRequest result) {
        serviceProvider.acceptRemoteResponse(ServiceName.parse(serviceName), result.getStatus());
    }
}
