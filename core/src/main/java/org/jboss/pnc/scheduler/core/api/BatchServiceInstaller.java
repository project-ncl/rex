package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;

import javax.transaction.*;

public interface BatchServiceInstaller {
    /**
     * Installs a service to the batch
     *
     * @param name unique name of the installed Service
     * @return ServiceBuilder used to install the Service
     */
    ServiceBuilder addService(ServiceName name);

    /**
     * Commits the changes and installs new Services to the Container
     */
    void commit();
}
