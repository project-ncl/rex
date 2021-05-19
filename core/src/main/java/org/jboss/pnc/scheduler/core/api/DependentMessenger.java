package org.jboss.pnc.scheduler.core.api;

/**
 * Dependent depends on one or more dependencies.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface DependentMessenger {
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
}
