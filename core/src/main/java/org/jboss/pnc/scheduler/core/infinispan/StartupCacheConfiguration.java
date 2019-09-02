package org.jboss.pnc.scheduler.core.infinispan;

import io.quarkus.runtime.StartupEvent;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class StartupCacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StartupCacheConfiguration.class);

    @Inject
    RemoteCacheManager cacheManager;

    void onStart(@Observes StartupEvent ev) {
        log.info("Checking for server configuration");
        RemoteCache cache;
        try {
            cache = cacheManager.getCache("near-tasks", TransactionMode.NON_DURABLE_XA);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid configuration cannot get TransactionManager, ", e);
        }
        if (cache == null) {
            throw new IllegalStateException("Cannot retrieve needed cache. Check your Infinispan server configuration.");
        }
    }

}
