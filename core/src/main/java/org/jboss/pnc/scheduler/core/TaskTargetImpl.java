package org.jboss.pnc.scheduler.core;

import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskTarget;

public abstract class TaskTargetImpl implements TaskTarget {

    @Override
    public BatchTaskInstaller addTasks() {
        return new BatchTaskInstallerImpl(this);
    }

    @Override
    public void removeTask(String task) {
        throw new UnsupportedOperationException("Currently not implemented");
    }

    protected abstract void install(BatchTaskInstallerImpl serviceBuilder);
}
