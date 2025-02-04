/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.rex.core.counter;

import io.quarkus.infinispan.client.Remote;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Running
@ApplicationScoped
public class RunningCounter implements Counter {

    public static final String RUNNING_KEY = "RUNNING";
    public static final String NAME_SEPARATOR = "-";

    @Remote("rex-counter")
    RemoteCache<String, Long> counterCache;

    private String resolveKey(String optionalKey) {
        if (optionalKey == null) {
            // DEFAULT QUEUE KEY
            return RUNNING_KEY;
        } else if (optionalKey.isBlank()) {
            throw new IllegalArgumentException("Counter key must not be blank");
        }

        // GENERATED NAMED QUEUE KEY
        return RUNNING_KEY + NAME_SEPARATOR + optionalKey;
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
        Map<String, Long> entries = counterCache.entrySet().stream()
                .filter(e -> e.getKey().startsWith(RUNNING_KEY))
                .collect(toMap(
                        entry -> entry.getKey()
                                .replaceFirst(RUNNING_KEY, "")
                                .replaceFirst(NAME_SEPARATOR, ""),
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
