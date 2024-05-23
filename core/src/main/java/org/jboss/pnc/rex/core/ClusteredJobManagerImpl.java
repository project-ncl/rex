/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.ResourceType;
import org.jboss.pnc.rex.core.api.ClusteredJobManager;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.api.ResourceHolder;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.core.jobs.TimeoutCancelClusterJob;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.NodeResource;

import java.util.List;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

@Slf4j
@ApplicationScoped
public class ClusteredJobManagerImpl implements ClusteredJobManager, ResourceHolder {

    private final ClusteredJobRegistry registry;

    private final Event<ClusteredJob> scheduleCJob;

    private final ApplicationConfig appConfig;

    public ClusteredJobManagerImpl(ClusteredJobRegistry registry,
                                   Event<ClusteredJob> scheduleCJob,
                                   ApplicationConfig appConfig) {
        this.registry = registry;
        this.scheduleCJob = scheduleCJob;
        this.appConfig = appConfig;
    }

    @Override
    public void enlist(ClusteredJobReference cjob) {
        registry.createWithOwnership(cjob);
    }

    @Override
    public void delist(String id) {
        if (isOwned(id)) {
            registry.delete(id);
        }
    }

    @Override
    public boolean isOwned(String id) {
        ClusteredJobReference job = registry.getById(id);

        return job != null && job.isOwnedBy(appConfig.name());
    }

    @Override
    public boolean exists(String id) {
        return registry.getById(id) != null;
    }

    @Override
    public List<NodeResource> getLocalResources() {
        return registry.getOwned().stream().map(ClusteredJobManagerImpl::toResource).toList();
    }

    private static NodeResource toResource(ClusteredJobReference cjob) {
        return NodeResource.builder()
            .ownerNode(cjob.getOwner())
            .resourceId(cjob.getId())
            .resourceType(ResourceType.CLUSTERED_JOB)
            .build();
    }

    @Override
    public List<NodeResource> getOwnedResources(String instanceName) {
        return registry.getByOwnership(instanceName).stream().map(ClusteredJobManagerImpl::toResource).toList();
    }

    @Override
    @Transactional(MANDATORY)
    public void registerResourceLocally(NodeResource resource) {
        if (!resource.getResourceType().equals(this.getResourceType())) {
            // skip irrelevant resources
            return;
        }
        var oldJob = registry.getById(resource.getResourceId());

        if (oldJob == null) {
            log.warn("Attempt to register a Clustered Job with id {} failed. It is missing in rex-cluster-jobs Cache.",
                resource.getResourceId());
            return;
        }

        // change the c-job owner to the local instance
        ClusteredJobReference newOwnedJob = oldJob.toBuilder().owner(appConfig.name()).build();

        // push the changes
        enlist(newOwnedJob);

        // THESE THINGS NEED TO BE ASYNC AND ONLY RUN ON SUCCESSFUL UNDERLYING TRANSACTION
        scheduleCJob.fire(switch (oldJob.getType()) {
            case CANCEL_TIMEOUT -> new TimeoutCancelClusterJob(newOwnedJob);
        });
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.CLUSTERED_JOB;
    }

}
