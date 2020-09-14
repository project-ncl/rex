package org.jboss.pnc.scheduler.core.api;

/**
 * Dependent depends on one or more dependencies.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface Dependent {
    /**
     * Notify this dependent that it's dependency has succeeded.
     */
    void dependencySucceeded();

    /**
     * Notify this dependent that it's dependency has stopped.
     */
    void dependencyStopped();

    /**
     * Notify this dependent that it's dependency has been cancelled.
     */
    void dependencyCancelled();

    /**
     * Notify this dependent that new dependency was created.
     *
     * @param dependency name of added dependency
     */
    void dependencyCreated(String dependency);
    /**
     * Gets unique name of an associated Task.
     *
     * @return the name
     */
    String getName();
}
