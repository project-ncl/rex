package org.jboss.pnc.scheduler.rest;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateServiceRequest;
import org.jboss.pnc.scheduler.facade.api.ServiceProvider;
import org.jboss.pnc.scheduler.rest.api.ServiceEndpoint;
import org.jboss.pnc.scheduler.rest.parameters.ServiceFilterParameters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

@ApplicationScoped
public class ServiceEndpointImpl implements ServiceEndpoint {

    private ServiceProvider serviceProvider;

    @Inject
    public ServiceEndpointImpl(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public List<ServiceDTO> create(@NotNull CreateServiceRequest request) {
        return serviceProvider.create(request.getServices());
    }

    @Override
    public List<ServiceDTO> getAll(ServiceFilterParameters filterParameters) {
        Boolean allFiltersAreFalse = !filterParameters.getFinished() && !filterParameters.getRunning() && !filterParameters.getWaiting();

        //If query is empty return all services
        if (allFiltersAreFalse) {
            return serviceProvider.getAll(true,true,true);
        }
        return serviceProvider.getAll(filterParameters.getWaiting(), filterParameters.getRunning(), filterParameters.getFinished());
    }

    @Override
    public ServiceDTO getSpecific(String serviceName) {
        return serviceProvider.get(ServiceName.parse(serviceName));
    }

    @Override
    public void cancel(String serviceName) {
        serviceProvider.cancel(ServiceName.parse(serviceName));
    }

 /*   @Override
    public List<ServiceDTO> getGraph(String serviceName) {
        return serviceProvider.getAllRelated(ServiceName.parse(serviceName));
    }*/
}
