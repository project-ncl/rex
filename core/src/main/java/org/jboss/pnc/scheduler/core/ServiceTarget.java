package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * Target where Services are install to and removed from.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceTarget {
    /**
     * Method server as a way to create new Services to install into ServiceTarget.
     * <p>
     * Returns ServiceBuilder that is used to create ServiceController that are installed in the Target
     * <p>
     * To install a Controller and start its scheduling use {@code ServiceBuilder.install()}
     *
     * @param service the unique ServiceName for new jobs/Service
     * @return the service builder
     */
    ServiceBuilder addService(ServiceName service);

    /**
     * Method serves as a way to update already installed Services
     *
     * Returns ServiceUpdater that can be used to alter behaviour of the Controller
     *
     * To commit the changes use {@code ServiceUpdater.commit()} ServiceUpdater.
     *
     * @param service the service
     * @return the service updater
     */
    ServiceUpdater updateService(ServiceName service);

    /**
     * Removes a service from the Target. It has to be in the {@code ServiceController.StateGroup.FINAL}
     *
     * @param service the unique service name
     */
    void removeService(ServiceName service);
}
