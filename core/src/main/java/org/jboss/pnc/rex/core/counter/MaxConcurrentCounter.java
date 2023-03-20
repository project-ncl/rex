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
package org.jboss.pnc.rex.core.counter;

import io.quarkus.infinispan.client.Remote;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;

import javax.enterprise.context.ApplicationScoped;

@MaxConcurrent
@ApplicationScoped
public class MaxConcurrentCounter implements Counter {

    public static final String MAX_COUNTER_KEY = "MAX_CONCURRENT";

    @Remote("rex-counter")
    RemoteCache<String, Long> counterCache;

    @ConfigProperty(name = "scheduler.options.concurrency.default", defaultValue = "5")
    String defaultMax;

    @Override
    public VersionedValue<Long> getMetadataValue() {
        VersionedValue<Long> metadata = counterCache.getWithMetadata(MAX_COUNTER_KEY);
        if(metadata == null) {
            initialize(Long.valueOf(defaultMax));
            metadata = counterCache.getWithMetadata(MAX_COUNTER_KEY);
        }
        return metadata;
    }

    @Override
    public boolean replaceValue(VersionedValue<Long> previousValue, Long value) {
        return counterCache.replaceWithVersion(MAX_COUNTER_KEY, value, previousValue.getVersion());
    }

    @Override
    public Long getValue() {
        return counterCache.get(MAX_COUNTER_KEY);
    }

    @Override
    public boolean replaceValue(Long previousValue, Long newValue) {
        return counterCache.replace(MAX_COUNTER_KEY, previousValue, newValue);
    }

    @Override
    public void initialize(Long initialValue) {
        counterCache.put(MAX_COUNTER_KEY, initialValue);
    }
}
