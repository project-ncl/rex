package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchServiceInstaller;
import org.jboss.pnc.scheduler.core.api.ServiceTarget;

import javax.transaction.*;

public abstract class ServiceTargetImpl implements ServiceTarget {

    public ServiceTargetImpl() {
    }

    @Override
    public BatchServiceInstaller addServices() {
        return new BatchServiceInstallerImpl(this);
    }

    @Override
    public void removeService(ServiceName service) {
        throw new UnsupportedOperationException("Currently not implemented");
    }

    protected abstract void install(BatchServiceInstallerImpl serviceBuilder);
}
