package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;

/**
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceBuilder {
    ServiceBuilder requires(ServiceName service);
    ServiceBuilder isRequiredBy(ServiceName service);
    ServiceBuilder serInitialMode();
    ServiceController install();
}
