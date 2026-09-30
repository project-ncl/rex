/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.model.ClusteredJobReference;

import java.util.List;

/**
 * Registry for interacting with persisted ClusteredJobReferences. Clustered Jobs are Jobs that are long-running and
 * susceptible to instance crashes. To counteract that, their 'references' are persisted. In case of a crash/shutdown,
 * Jobs are failed-over to another instance. It is required that the Job (under package org.jboss.pnc.rex.core.jobs) can
 * be instantiated from just the persisted reference.
 *
 * Every job has an owner, which is the instance that created it. The instance id is defined in application configuration.
 */
public interface ClusteredJobRegistry {

    /**
     * Returns all cluster job references across all instances.
     *
     * @return list of references
     */
    List<ClusteredJobReference> getAll();

    /**
     * Returns cluster job reference associated with a task.
     *
     * @param taskId task id
     * @return list of references
     */
    List<ClusteredJobReference> getByTask(String taskId);

    /**
     * Returns all job references that are local to the instance.
     *
     * @return list of references
     */
    List<ClusteredJobReference> getOwned();

    /**
     * Returns all job references owned by the instance specified by ID.
     *
     * @param instanceId instance id
     * @return list of references
     */
    List<ClusteredJobReference> getByOwnership(String instanceId);

    /**
     * Returns concrete reference specified by unique ID.
     *
     * @param id reference id
     * @return reference
     */
    ClusteredJobReference getById(String id);

    /**
     * Persists the supplied reference to the registry. The reference has to have the 'owner' set to the local instance.
     *
     * @param jobReference
     * @return
     */
    String createWithOwnership(ClusteredJobReference jobReference);

    /**
     * Removes job id reference from the registry.
     *
     * @param jobId reference id
     */
    void delete(String jobId);

    /**
     * Removes all persisted jobs from the registry.
     */
    void deleteAll();
}
