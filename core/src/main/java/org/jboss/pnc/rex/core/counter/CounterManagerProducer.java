/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.counter.api.CounterManager;

/**
 * Exposes the Infinispan {@link CounterManager} (clustered counters) from the Hot Rod
 * {@link RemoteCacheManager} that Quarkus produces for the default Infinispan client. The counter
 * subsystem lives inside the same Infinispan cluster — no additional deployment is required.
 */
@ApplicationScoped
public class CounterManagerProducer {

    @Produces
    @ApplicationScoped
    public CounterManager counterManager(RemoteCacheManager cacheManager) {
        return RemoteCounterManagerFactory.asCounterManager(cacheManager);
    }
}
