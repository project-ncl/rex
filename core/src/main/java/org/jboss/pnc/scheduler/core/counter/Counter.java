package org.jboss.pnc.scheduler.core.counter;

import org.infinispan.client.hotrod.MetadataValue;

public interface Counter {

    MetadataValue<Long> getMetadataValue();

    boolean replaceValue(MetadataValue<Long> previousValue, Long newValue);

    Long getValue();

    boolean replaceValue(Long previousValue, Long value);

    void initialize(Long initialValue);
}
