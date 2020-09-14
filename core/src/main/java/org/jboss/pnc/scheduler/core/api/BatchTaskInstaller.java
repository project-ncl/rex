package org.jboss.pnc.scheduler.core.api;

public interface BatchTaskInstaller {
    /**
     * Installs a task to the batch
     *
     * @param name unique name of the installed Task
     * @return ServiceBuilder used to install the Task
     */
    TaskBuilder addTask(String name);

    /**
     * Commits the changes and installs new Services to the Container
     */
    void commit();
}
