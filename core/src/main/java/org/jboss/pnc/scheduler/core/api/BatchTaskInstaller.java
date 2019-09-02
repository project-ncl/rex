package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;

public interface BatchTaskInstaller {
    /**
     * Installs a task to the batch
     *
     * @param name unique name of the installed Task
     * @return ServiceBuilder used to install the Task
     */
    TaskBuilder addTask(ServiceName name);

    /**
     * Commits the changes and installs new Services to the Container
     */
    void commit();
}
