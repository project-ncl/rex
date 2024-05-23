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

import io.quarkus.arc.All;
import io.quarkus.infinispan.client.Remote;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.TransactionExceptionResult;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.jboss.pnc.rex.core.api.ResourceHolder;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.model.NodeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class FailoverInitiator {

    private static final Logger log = LoggerFactory.getLogger(FailoverInitiator.class);

    private final List<ResourceHolder> resourceHolders;

    private final RemoteCache<String, NodeResource> signal;

    private final ApplicationConfig appConfig;

    private final ManagedExecutor executor;

    private final AtomicBoolean skipTakeovers = new AtomicBoolean(false);

    public FailoverInitiator(@All List<ResourceHolder> failoverManager,
                             @Remote("rex-signals") RemoteCache<String, NodeResource> signalsCache,
                             ApplicationConfig appConfig,
                             ManagedExecutor executor) {
        this.resourceHolders = failoverManager;
        this.signal = signalsCache;
        this.appConfig = appConfig;
        this.executor = executor;

        // register client listener
        signalsCache.addClientListener(this.new ResourceFailoverInfinispanListener());
    }

    //FIXME implement regular healthchecks for node instances.

    /**
     * On start, look whether there are a
     */
    @Startup(ObserverMethod.DEFAULT_PRIORITY+10) // Startup method that initializes caches must be run beforehand
    public void takeAvailableResources() {
        try (var resourceIDs = signal.keySet().stream()){
            resourceIDs.forEach(resourceID -> tryToTakeoverResource(resourceID, log));
        }
    }

    @Transactional
    @Shutdown
    public void failoverResources() {
        skipTakeovers.set(true);

        log.info("Failover initiated");
        List<? extends NodeResource> localResources = resourceHolders.stream()
            .map(ResourceHolder::getLocalResources)
            .flatMap(List::stream)
            .toList();

        // this will trigger ResourceFailoverListeners for other node instances
        // it can be thought of as broadcast to other instances
        localResources.forEach(localResource -> signal.put(localResource.getResourceId(), localResource));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void clearCaches() {
        signal.clear();
    }

    public Map<String, NodeResource> signal() {
        return signal;
    }

    @ClientListener
    public class ResourceFailoverInfinispanListener {

        private static final Logger listenerLog = LoggerFactory.getLogger(ResourceFailoverInfinispanListener.class);


        @ClientCacheEntryCreated
        public void onClientCacheEntryCreated(ClientCacheEntryCreatedEvent<String> event) {
            listenerLog.debug("Got client cache entry created event: {}", event);
            executor.submit(() -> tryToTakeoverResource(event.getKey(), listenerLog));
        }
    }

    private void tryToTakeoverResource(String resourceId, Logger logger) {
        QuarkusTransaction.requiringNew()
            .exceptionHandler(throwable -> exceptionHandler(throwable, logger))
            .run(() -> {
                logger.debug("Trying to takeover resource {}", resourceId);
                if (skipTakeovers.get()) {
                    log.debug("Skipping resource {}. In process of shutting down.", resourceId);
                }
                MetadataValue<NodeResource> meta = signal.getWithMetadata(resourceId);
                if (meta == null) {
                    log.debug("No entry found for resource {}. It was most likely already taken by other node.", resourceId);
                    return;
                }

                NodeResource resource = meta.getValue();
                if (resource.getOwnerNode().equals(appConfig.name())) {
                    logger.debug("Skipping failover of previously owned resource.");
                    return;
                }

                boolean b = signal.removeWithVersion(resourceId, meta.getVersion());
                if (!b) {
                    throw new IllegalStateException("Could not remove resource " + resourceId + " from signal");
                }

                resourceHolders.stream()
                    .filter(holder -> holder.getResourceType().equals(resource.getResourceType()))
                    .forEach(holder -> holder.registerResourceLocally(resource));
            });
    }

    private static TransactionExceptionResult exceptionHandler(Throwable throwable, Logger logger) {
        logger.debug("Resource takeover failed. Another instance won. Exc: {}, Cause: {}",
            throwable.getMessage(),
            throwable.getCause() != null ? throwable.getCause().getMessage() : "NO CAUSE");

        return TransactionExceptionResult.ROLLBACK;
    }
}
