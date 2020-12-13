package org.jboss.pnc.scheduler.core.api;

/**
 * Dependent depends on one or more dependencies.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface Dependent {
    /**
     * Notify this dependent that it's dependency has succeeded.
     * @param name
     */
    void dependencySucceeded(String name);

    /**
     * Notify this dependent that it's dependency has stopped.
     * @param name
     */
    void dependencyStopped(String name);

    /**
     * Notify this dependent that it's dependency has been cancelled.
     * @param name
     */
    void dependencyCancelled(String name);

    /**
     * Notify this dependent that new dependency was created.
     *
     * @param name
     * @param dependency name of added dependency
     */
    void dependencyCreated(String name, String dependency);
}
