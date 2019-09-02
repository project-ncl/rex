package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * Updater for already installed jobs(Services)
 *
 * TODO Just a concept
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceUpdater {
    /**
     * Remove dependency.
     *
     * @param service the service
     * @return the service updater
     */
    ServiceUpdater removeDependency(ServiceName service);

    /**
     * Remove dependant.
     *
     * @param service the service
     * @return the service updater
     */
    ServiceUpdater removeDependant(ServiceName service);

    /**
     * Add dependency.
     *
     * @param service the service
     * @return the service updater
     */
    ServiceUpdater addDependency(ServiceName service);

    /**
     * Add dependant.
     *
     * @param service the service
     * @return the service updater
     */
    ServiceUpdater addDependant(ServiceName service);

    /**
     * Commit the changes.
     */
    void commit();
}
