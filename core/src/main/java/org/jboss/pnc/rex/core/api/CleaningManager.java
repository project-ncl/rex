/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

/**
 * Independent manager for cleaning/disposing of Tasks in Rex.
 */
public interface CleaningManager {

    /**
     * Queries and deletes all Tasks that can be removed. The tasks have to be marked as disposable
     * {@link org.jboss.pnc.rex.model.Task#disposable} and have no dependants. One-by-one the deletion is triggered on
     * these tasks.
     *
     * Each deletion cascades on dependencies but the same conditions apply (marked + no dependants). For
     * the cascaded dependencies, the dependant from which the deletion was triggered is at the time of triggering
     * already removed (to retain conditions for removal).
     *
     * The method can be called at any time, even when there is nothing to clean.
     */
    void tryClean();
}
