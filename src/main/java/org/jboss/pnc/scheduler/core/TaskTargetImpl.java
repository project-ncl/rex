package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskTarget;

public abstract class TaskTargetImpl implements TaskTarget {

    public TaskTargetImpl() {
    }

    @Override
    public BatchTaskInstaller addTasks() {
        return new BatchTaskInstallerImpl(this);
    }

    @Override
    public void removeTask(ServiceName task) {
        throw new UnsupportedOperationException("Currently not implemented");
    }

    protected abstract void install(BatchTaskInstallerImpl serviceBuilder);
}
