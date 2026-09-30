/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

/**
 * Interface for communicating/messaging dependencies (tasks you depend on).
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface DependencyMessenger {

    /**
     * Send a signal to your dependency that you have been removed. This usually means that you want your dependency
     * also removed.
     *
     * @param name name of a dependency Task you send a message to
     */
    void dependantDeleted(String name, String deletedDependant);

    /**
     * Send a signal to your depenedncy that you've finished rollback. It may cause the dependency to begin it's remote
     * rollback.
     *
     * @param name name of a dependency Task you send a message to
     */
    void dependantRolledBack(String name);
}
