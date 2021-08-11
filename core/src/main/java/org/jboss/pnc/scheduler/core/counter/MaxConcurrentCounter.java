package org.jboss.pnc.scheduler.core.counter;

import io.quarkus.infinispan.client.Remote;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;

import javax.enterprise.context.ApplicationScoped;

@MaxConcurrent
@ApplicationScoped
public class MaxConcurrentCounter implements Counter {

    public static final String MAX_COUNTER_KEY = "MAX_CONCURRENT";

    @Remote("counter")
    RemoteCache<String, Long> counterCache;

    @ConfigProperty(name = "scheduler.options.concurrency.default", defaultValue = "5")
    String defaultMax;

    @Override
    public MetadataValue<Long> getMetadataValue() {
        MetadataValue<Long> metadata = counterCache.getWithMetadata(MAX_COUNTER_KEY);
        if(metadata == null) {
            initialize(Long.valueOf(defaultMax));
            metadata = counterCache.getWithMetadata(MAX_COUNTER_KEY);
        }
        return metadata;
    }

    @Override
    public boolean replaceValue(MetadataValue<Long> previousValue, Long value) {
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
