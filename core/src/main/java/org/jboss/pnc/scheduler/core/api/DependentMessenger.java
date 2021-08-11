package org.jboss.pnc.scheduler.core.api;

/**
 * Interface for communicating/messaging dependents (tasks that depend on you).
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface DependentMessenger {
    /**
     * Notify this dependent that it's dependency has succeeded.
     * @param name name of the dependent
     */
    void dependencySucceeded(String name);

    /**
     * Notify this dependent that it's dependency has stopped.
     * @param name name of the dependent
     */
    void dependencyStopped(String name);

    /**
     * Notify this dependent that it's dependency has been cancelled.
     * @param name name of the dependent
     */
    void dependencyCancelled(String name);
}
