package org.jboss.pnc.rex.core.counter;

import org.infinispan.client.hotrod.MetadataValue;

/**
 * Interface for interacting with counter. Use Metadata versions of get/replace methods to avoid concurrent updates in
 * ISPN.
 */
public interface Counter {

    MetadataValue<Long> getMetadataValue();

    boolean replaceValue(MetadataValue<Long> previousValue, Long newValue);

    @Deprecated
    Long getValue();

    @Deprecated
    boolean replaceValue(Long previousValue, Long value);

    void initialize(Long initialValue);
}
