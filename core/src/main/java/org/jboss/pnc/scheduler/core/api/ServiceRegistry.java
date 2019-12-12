
package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.exceptions.ServiceNotFoundException;
import org.jboss.pnc.scheduler.core.model.Service;

import java.util.Collection;
import java.util.List;

/**
 * The registry used to retrieve Services.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceRegistry {
    /**
     * Returns the Controller for a particular job(Service). Returns {@code null} if not found.
     *
     * @param service the serviceName of the service
     * @return the service controller or null if doesn't exist
     */
    ServiceController getServiceController(ServiceName service);

    /**
     * Returns the Controller for a particular job(Service). Throws an exception if not found.
     *
     * @param service the service name
     * @return the service controller
     * @throws ServiceNotFoundException the service was not found
     */
    ServiceController getRequiredServiceController(ServiceName service) throws ServiceNotFoundException;

    /**
     * Returns the Service for a unique ServiceName.
     *
     * @param service the serviceName of the service
     * @return the service or null if doesn't exist
     */
    Service getService(ServiceName service);

    /**
     * Returns the Service for a unique ServiceName. Throws an exception if not found.
     *
     * @param service the service name
     * @return the service
     * @throws ServiceNotFoundException the service was not found
     */
    Service getRequiredService(ServiceName service) throws ServiceNotFoundException;

    /**
     * Returns all Services present in the cache filtered by parameters
     *
     * (Can be costly without filters)
     *
     * @param waiting is in StateGroup.IDLE state
     * @param running is in StateGroup.RUNNING state
     * @param finished is in StateGroup.FINAL state
     * @return list of filtered services
     */
    List<Service> getServices(boolean waiting, boolean running, boolean finished);

    /**
     * Returns all services in clustered container.
     *
     * @return the service names
     */
    Collection<ServiceName> getServiceNames();
}
