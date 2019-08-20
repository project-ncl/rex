package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceTarget {
    ServiceBuilder addService(ServiceName service);
    void removeService(ServiceName service);
}
