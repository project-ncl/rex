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
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;

import javax.enterprise.context.ApplicationScoped;

@Running
@ApplicationScoped
public class RunningCounter implements Counter {

    public static final String RUNNING_KEY = "RUNNING";

    @Remote("counter")
    RemoteCache<String, Long> counterCache;

    @Override
    public MetadataValue<Long> getMetadataValue() {
        MetadataValue<Long> metadata = counterCache.getWithMetadata(RUNNING_KEY);
        if (metadata == null) {
            initialize(0L);
            metadata = counterCache.getWithMetadata(RUNNING_KEY);
        }
        return metadata;
    }

    @Override
    public boolean replaceValue(MetadataValue<Long> previousValue, Long value) {
        return counterCache.replaceWithVersion(RUNNING_KEY, value, previousValue.getVersion());
    }

    @Override
    public Long getValue() {
        return counterCache.get(RUNNING_KEY);
    }

    @Override
    public boolean replaceValue(Long previousValue, Long newValue) {
        return counterCache.replace(RUNNING_KEY, previousValue, newValue);
    }

    @Override
    public void initialize(Long initialValue) {
        counterCache.put(RUNNING_KEY, initialValue);
    }
}
