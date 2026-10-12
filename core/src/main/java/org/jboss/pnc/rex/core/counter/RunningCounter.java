/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.pnc.rex.core.common.Constants;

import io.quarkus.infinispan.client.Remote;

@Running
@ApplicationScoped
public class RunningCounter implements Counter {

    @Remote("rex-counter")
    RemoteCache<String, Long> counterCache;

    private String resolveKey(String optionalKey) {
        if (optionalKey == null) {
            // DEFAULT QUEUE KEY
            return Constants.RUNNING_COUNTER_KEY;
        } else if (optionalKey.isBlank()) {
            throw new IllegalArgumentException("Counter key must not be blank");
        }

        // GENERATED NAMED QUEUE KEY
        return Constants.RUNNING_COUNTER_KEY + Constants.NAME_SEPARATOR + optionalKey;
    }

    @Override
    public VersionedValue<Long> getMetadataValue(String key) {
        VersionedValue<Long> metadata = counterCache.getWithMetadata(resolveKey(key));

        // init default queue
        if (metadata == null && key == null) {
            initialize(null, 0L);
            metadata = counterCache.getWithMetadata(resolveKey(null));
        }

        return metadata;
    }

    @Override
    public boolean replaceValue(String key, VersionedValue<Long> previousValue, Long value) {
        return counterCache.replaceWithVersion(resolveKey(key), value, previousValue.getVersion());
    }

    @Override
    public Long getValue(String key) {
        return counterCache.get(resolveKey(key));
    }

    @Override
    public boolean replaceValue(String key, Long previousValue, Long newValue) {
        return counterCache.replace(resolveKey(key), previousValue, newValue);
    }

    @Override
    public void initialize(String key, Long initialValue) {
        counterCache.put(resolveKey(key), initialValue);
    }

    @Override
    public Map<String, Long> entries() {
        Map<String, Long> entries = counterCache.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(Constants.RUNNING_COUNTER_KEY))
                .collect(
                        toMap(
                                entry -> entry.getKey()
                                        .replaceFirst(Constants.RUNNING_COUNTER_KEY, "")
                                        .replaceFirst(Constants.NAME_SEPARATOR, ""),
                                Map.Entry::getValue));

        // should be always true, unless default queue was never initialized
        if (entries.containsKey("")) {
            // make the default queue key be 'null' by which it is accessed in upper-layer methods
            Long defaultQueueValue = entries.remove("");
            entries.put(null, defaultQueueValue);
        }
        return entries;
    }
}
