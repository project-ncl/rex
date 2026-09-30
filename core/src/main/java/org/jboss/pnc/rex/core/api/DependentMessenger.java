/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

/**
 * Interface for communicating/messaging dependents (tasks that depend on you).
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface DependentMessenger {
    /**
     * Notify this dependent that its dependency has succeeded.
     * @param name name of the dependent
     */
    void dependencySucceeded(String name);

    /**
     * Notify this dependent that its dependency has stopped.
     * @param name name of the dependent
     */
    void dependencyStopped(String name, String cause);

    /**
     * Notify this dependent that its dependency has been cancelled.
     * @param name name of the dependent
     */
    void dependencyCancelled(String name, String cause);

    /**
     * Notify this dependent that its dependency's notification failed.
     * @param name name of the dependent
     */
    void dependencyNotificationFailed(String name, String cause);

    /**
     * Notify this dependent that its dependency has reset after rollback.
     * @param name name of the dependent
     */
    void dependencyReset(String name);

    /**
     * Notify this dependent that its dependency is preparing to start rollback process. If dependent is marked to
     * rollback, it will also transition to rollback-related state.
     * @param name
     */
    void dependencyIsToRollback(String name);
}
