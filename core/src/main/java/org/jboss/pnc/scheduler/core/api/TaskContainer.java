package org.jboss.pnc.scheduler.core.api;

/**
 * The interface Task container. Container is a registry and target for installations.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskContainer extends TaskRegistry, TaskTarget {
    /**
     * Initiates 'graceful' shutdown of the container. Currently, not implemented.
     */
    void shutdown();

    /**
     * Gets the name of the container/node
     *
     * @return name of the instance
     */
    String getDeploymentName();

    /**
     * Returns true is container is shutting down. Currently, not implemented.
     *
     * @return boolean
     */
    boolean isShutdown();
}
