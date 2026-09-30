/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.common.enums.ResourceType;
import org.jboss.pnc.rex.core.FailoverInitiator;
import org.jboss.pnc.rex.model.NodeResource;

import java.util.List;

/**
 * The implementor contains a critical local resource that needs to be failed over in case the Rex instance gets
 * shutdown or is unavailable for a longer period of time.
 *
 * Currently, these methods are called on @Shutdown of the local instance in {@link FailoverInitiator}
 */
public interface ResourceHolder {

    /**
     * Returns all resources owned by the local instance/node.
     *
     * @return list of node resources
     */
    List<NodeResource> getLocalResources();

    /**
     * Returns all resources owned by a specific instance/node.
     *
     * @param instanceName name of a unique instance
     * @return list of node resources
     */
    List<NodeResource> getOwnedResources(String instanceName);

    /**
     * The instance registers a resource locally. This means that the {@param resource} will have to be instantiated
     * (f.e. Clustered Job needs to be started) and the owner has to be changed to the local instance.
     *
     * @param resource resource to register
     */
    void registerResourceLocally(NodeResource resource);

    /**
     * The type of resource that can be failed-over.
     *
     * @return resource type
     */
    ResourceType getResourceType();
}
