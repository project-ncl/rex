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
package org.jboss.pnc.rex.core.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.Startup;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.NodeResource;
import org.jboss.pnc.rex.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartCachesOnStartup {
    private static final Logger log = LoggerFactory.getLogger(StartCachesOnStartup.class);

    @Inject
    @Remote("rex-tasks")
    RemoteCache<String, Task> tasks;

    @Inject
    @Remote("rex-constraints")
    RemoteCache<String, String> constraints;

    @Inject
    @Remote("rex-counter")
    RemoteCache<String, Long> counters;

    @Inject
    @Remote("rex-cluster-jobs")
    RemoteCache<String, ClusteredJobReference> clusterJobs;

    @Inject
    @Remote("rex-signals")
    RemoteCache<String, NodeResource> signal;

    @Startup
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
