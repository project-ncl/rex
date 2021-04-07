package org.jboss.pnc.scheduler.core.api;

/**
 * The interface Task container.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskContainer extends TaskRegistry, TaskTarget {
    /**
     * Initiates 'graceful' shutdown of the container
     */
    void shutdown();

    /**
     * Gets the name of the container/node
     *
     * @return the name
     */
    String getDeploymentName();

    /**
     * Returns true is container is shutting down.
     *
     * @return boolean
     */
    boolean isShutdown();
}
