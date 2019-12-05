package org.jboss.pnc.scheduler.facade.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.dto.ServiceDTO;

import java.util.List;

public interface ServiceProvider {

    /**
     * Creates and starts scheduling of a Set of services
     *
     * Services have to be new but they can have links to existing services and each other.
     *
     * @param services set of services to be scheduled
     * @return list of created services
     */
    List<ServiceDTO> create(List<ServiceDTO> services);

    /**
     * returns all services based on filter
     *
     * @return set of services
     */
    List<ServiceDTO> getAll(boolean waiting, boolean running, boolean finished);

    /**
     * Cancels execution of the service and its dependants
     *
     * @param serviceName existing service
     */
    void cancel(ServiceName serviceName);

    /**
     * Returns existing service based on param
     *
     * @param serviceName name of existing service
     * @return service entity
     */
    ServiceDTO get(ServiceName serviceName);

    /**
     * Returns all related services
     * (all dependants, all dependencies, dependants of dependencies, dependencies of dependants)
     *
     * @param serviceName name of existing service
     * @return set of related services
     */
    List<ServiceDTO> getAllRelated(ServiceName serviceName);

    /**
     * Used for communication with remote entity. Invoked by remote entity by provided callback.
     *
     * @param positive callback is positive or negative
     *          true == remote entity responds that it has finished execution of the service
     *          false == remote entity responds that the service has failed its execution
     */
    void acceptRemoteResponse(ServiceName serviceName, boolean positive);
}
