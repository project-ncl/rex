package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceRegistry {
    ServiceController getRequiredService(ServiceName service);
    ServiceController getService(ServiceName service);
    ServiceName getServiceNames();
}
