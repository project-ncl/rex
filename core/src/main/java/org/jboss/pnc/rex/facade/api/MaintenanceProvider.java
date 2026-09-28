/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.facade.api;

/**
 * General provider for be-all end-all maintenance purposes of Rex service.
 */
public interface MaintenanceProvider {

    /**
     * The method brings back Rex to an initial state without Tasks. The Queues, internal Counters and all internal
     * caches are brought back to their factory state.
     *
     * In general 'settings' keep their value (even though they can change and have factory value). For example The
     * value of setting for Maximum Queue size is kept and not reset to initial value from config.
     *
     * Use this method with caution. The results are irreversible.
     */
    void clearEverything();
}
