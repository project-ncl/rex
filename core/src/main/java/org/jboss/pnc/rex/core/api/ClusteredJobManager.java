/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.model.ClusteredJobReference;

/**
 * ClusteredJobManager is the facade around managing ClusteredJobs. It handles enlisting and delisting.
 */
public interface ClusteredJobManager {

    /**
     * The method enlists(persists) the supplied job reference instance. The instance must have owner set to the ID of
     * local instance, otherwise an exception is thrown.
     *
     * @param cjob job reference instance
     */
    void enlist(ClusteredJobReference cjob);

    /**
     * The method delists the supplied job reference. The instance MUST OWN the reference, otherwise an exception is
     * thrown.
     *
     * @param id job id
     */
    void delist(String id);

    /**
     * The method returns true is the local instance exists and owns the job reference.
     *
     * @param id job id
     * @return true if local instance owns the job
     */
    boolean isOwned(String id);

    /**
     * The method returns true if the job reference exists in the registry.
     *
     * @param id
     * @return
     */
    boolean exists(String id);
}
