/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.infinispan;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.NodeResource;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.Startup;

@ApplicationScoped
public class StartCachesOnStartup {
    private static final Logger log = LoggerFactory.getLogger(StartCachesOnStartup.class);

    private final RemoteCache<String, Task> tasks;

    private final RemoteCache<String, String> constraints;

    private final RemoteCache<String, Long> counters;

    private final RemoteCache<String, ClusteredJobReference> clusterJobs;

    private final RemoteCache<String, NodeResource> signal;

    public StartCachesOnStartup(
            @Remote("rex-tasks") RemoteCache<String, Task> tasks,
            @Remote("rex-constraints") RemoteCache<String, String> constraints,
            @Remote("rex-counter") RemoteCache<String, Long> counters,
            @Remote("rex-cluster-jobs") RemoteCache<String, ClusteredJobReference> clusterJobs,
            @Remote("rex-signals") RemoteCache<String, NodeResource> signal) {
        this.tasks = tasks;
        this.constraints = constraints;
        this.counters = counters;
        this.clusterJobs = clusterJobs;
        this.signal = signal;
    }

    @Startup(ObserverMethod.DEFAULT_PRIORITY - 1)
    void onStart() {
        log.info("Startup: Initializing ISPN caches!");
        try {
            tasks.get("ASD");
            constraints.get("ASD");
            counters.get("ASD");
            clusterJobs.get("ASD");
            signal.get("ASD");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot get caches", e);
        }
    }

}
