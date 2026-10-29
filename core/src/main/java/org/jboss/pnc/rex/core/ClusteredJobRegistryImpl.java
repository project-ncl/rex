/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.model.ClusteredJobReference;

import io.quarkus.infinispan.client.Remote;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ClusteredJobRegistryImpl implements ClusteredJobRegistry {

    private final RemoteCache<String, ClusteredJobReference> jobs;

    private final ApplicationConfig appConfig;

    public ClusteredJobRegistryImpl(
            @Remote("rex-cluster-jobs") RemoteCache<String, ClusteredJobReference> jobCache,
            ApplicationConfig appConfig) {
        this.jobs = jobCache;
        this.appConfig = appConfig;
    }

    @Override
    public List<ClusteredJobReference> getAll() {
        return jobs.<ClusteredJobReference> query("FROM rex_model.ClusteredJobReference")
                .list();
    }

    @Override
    public List<ClusteredJobReference> getByTask(String taskId) {
        return jobs.<ClusteredJobReference> query("FROM rex_model.ClusteredJobReference WHERE taskName = :taskName")
                .setParameter("taskName", taskId)
                .list();
    }

    @Override
    public List<ClusteredJobReference> getOwned() {
        return getByOwnership(appConfig.name());
    }

    @Override
    public List<ClusteredJobReference> getByOwnership(String instanceName) {
        if (instanceName == null) {
            throw new IllegalArgumentException("Instance name cannot be null");
        }

        return jobs.<ClusteredJobReference> query("FROM rex_model.ClusteredJobReference WHERE owner = :instanceName")
                .setParameter("instanceName", instanceName)
                .list();
    }

    @Override
    public ClusteredJobReference getById(String id) {
        return jobs.get(id);
    }

    @Override
    @Transactional(MANDATORY)
    public String createWithOwnership(ClusteredJobReference jobReference) {
        if (!jobReference.getOwner().equals(appConfig.name())) {
            throw new IllegalArgumentException("ClusteredJobReference owner does not refer to the instanceName");
        }

        ClusteredJobReference put = jobs.withFlags(Flag.FORCE_RETURN_VALUE)
                .put(jobReference.getId(), jobReference);

        if (put != null && put.equals(jobReference)) {
            log.info(
                    "Re-registering owner for CJob {}\n\t Prev CJob: {} \n\t New CJob {}",
                    jobReference.getId(),
                    put,
                    jobReference);
        }

        return jobReference.getId();
    }

    public String forceCreate(ClusteredJobReference jobReference) {
        ClusteredJobReference put = jobs
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .put(jobReference.getId(), jobReference);

        return jobReference.getId();
    }

    @Override
    @Transactional(MANDATORY)
    public void delete(String jobId) {
        jobs.remove(jobId);
    }

    @Override
    @Transactional(MANDATORY)
    public void deleteAll() {
        jobs.clear();
    }
}
