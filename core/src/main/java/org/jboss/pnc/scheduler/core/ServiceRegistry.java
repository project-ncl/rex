package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;

import java.util.Collection;

/**
 * The registry used to retrieve ServiceController.
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
    ServiceController getService(ServiceName service);

    /**
     * Returns the Controller for a particular job(Service). Throws an exception if not found.
     *
     * @param service the service
     * @return the service controller
     * @throws ServiceNotFoundException the service not found exception
     */
    ServiceController getRequiredService(ServiceName service) throws ServiceNotFoundException;

    /**
     * Returns all services in clustered container.
     *
     * @return the service names
     */
    Collection<ServiceName> getServiceNames();
}
