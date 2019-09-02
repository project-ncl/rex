package org.jboss.pnc.scheduler.core;

/**
 * The interface Service container.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceContainer extends ServiceRegistry, ServiceTarget {
    /**
     * Initiates 'graceful' shutdown of the container
     */
    void shutdown();

    /**
     * Gets the name of the container/node
     *
     * @return the name
     */
    String getName();

    /**
     * Returns true is container is shutting down.
     *
     * @return boolean
     */
    boolean isShutdown();
}
