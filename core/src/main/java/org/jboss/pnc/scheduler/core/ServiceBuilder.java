package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * ServiceBuilders are used for creating jobs(Services) and their installment into the container
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceBuilder {
    /**
     * Registers a dependency(child)
     *
     * @param service the service
     * @return the service builder
     */
    ServiceBuilder requires(ServiceName dependency);

    /**
     * Registers a dependant(parent)
     *
     * @param service the service
     * @return the service builder
     */
    ServiceBuilder isRequiredBy(ServiceName dependant);

    /**
     * Set initial mode
     *
     * @return the service builder
     */
    ServiceBuilder serInitialMode(ServiceController.Mode mode);

    /**
     * Install the service to a Container.
     *
     * @return the service controller
     */
    ServiceController install();
}
