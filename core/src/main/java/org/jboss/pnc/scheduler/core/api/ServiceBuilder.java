package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.model.Mode;

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
     * @param mode initialMode (default is Mode.IDLE)
     * @return the service builder
     */
    ServiceBuilder setInitialMode(Mode mode);

    /**
     * Set payload that is sent to remote entity
     *
     * @param payload generic payload
     * @return the service builder
     */
    ServiceBuilder setPayload(String payload);

    /**
     * Commit changes to the BatchServiceBuilder
     *
     */
    void install();
}
