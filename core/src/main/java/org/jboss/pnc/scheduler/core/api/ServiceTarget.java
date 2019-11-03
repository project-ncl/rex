package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;

/**
 * Target where Services are installed to and removed from.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceTarget {
    /**
     * Method server as a way to create new Services to install into ServiceTarget.
     * <p>
     * Returns BatchServiceInstaller that is used to create Services that are installed in the Target
     * <p>
     *
     * To install a Controller and start its scheduling:
     *  1. use {@code BatchServiceInstaller.addService()}.
     *  2. declare the Service through returned ServiceBuilder and
     *  3. use ServiceBuilder.install() to commit the changes to the batch
     *  4. use {@code BatchServiceInstaller.commit()} to commit the batch to the target start scheduling
     *
     * @param service the unique ServiceName for new jobs/Service
     * @return the service builder
     */
    BatchServiceInstaller addServices();

    /**
     * Removes a service from the Target. It has to be in the {@code ServiceController.StateGroup.FINAL}
     *
     * @param service the unique service name
     */
    void removeService(ServiceName service);
}
