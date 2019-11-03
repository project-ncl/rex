package org.jboss.pnc.scheduler.core.infinispan;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientListener
     public class CacheListeners {
        Logger log = LoggerFactory.getLogger(CacheListeners.class);

        @ClientCacheEntryCreated
        public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
            log.info("Someone has created an entry: " + e);
        }

        @ClientCacheEntryModified
        public void handleModifiedEvent(ClientCacheEntryModifiedEvent e) {
            log.info("Someone has modified an entry: " + e);
        }

        @ClientCacheEntryRemoved
        public void handleRemovedEvent(ClientCacheEntryRemovedEvent e) {
            log.info("Someone has removed an entry: " + e);
        }

    }

