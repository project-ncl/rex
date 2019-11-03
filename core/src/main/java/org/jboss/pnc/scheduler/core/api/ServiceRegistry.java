
package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.exceptions.ServiceNotFoundException;
import org.jboss.pnc.scheduler.core.model.Service;

import java.util.Collection;

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
     * Returns the Service for a unique ServiceName.
     *
     * @param service the serviceName of the service
     * @return the service or null if doesn't exist
     */
    Service getService(ServiceName service);

    /**
     * Returns the Controller for a particular job(Service). Throws an exception if not found.
     *
     * @param service the service name
     * @return the service controller
     * @throws ServiceNotFoundException the service was not found
     */
    ServiceController getRequiredServiceController(ServiceName service) throws ServiceNotFoundException;

    /**
     * Returns the Service for a unique ServiceName. Throws an exception if not found.
     *
     * @param service the service name
     * @return the service
     * @throws ServiceNotFoundException the service was not found
     */
    Service getRequiredService(ServiceName service) throws ServiceNotFoundException;

    /**
     * Returns all services in clustered container.
     *
     * @return the service names
     */
    Collection<ServiceName> getServiceNames();
}
