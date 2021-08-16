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
