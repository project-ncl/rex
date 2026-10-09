/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import java.util.Map;

import jakarta.annotation.Nullable;

import org.infinispan.client.hotrod.VersionedValue;

/**
 * Interface for interacting with counter. Use Metadata versions of get/replace methods to avoid concurrent updates in
 * ISPN.
 */
public interface Counter {

    VersionedValue<Long> getMetadataValue(@Nullable String key);

    boolean replaceValue(@Nullable String key, VersionedValue<Long> previousValue, Long newValue);

    @Deprecated
    Long getValue(@Nullable String key);

    @Deprecated
    boolean replaceValue(@Nullable String key, Long previousValue, Long value);

    void initialize(@Nullable String key, Long initialValue);

    /**
     * USE WITH CAUTION, due to a bug in INFINISPAN the contents are not updated if there are changes in the Counter
     * during a transaction. (e.g. it will return the same entries regardless of modifications in the Counter)
     *
     * 'null' key is the DEFAULT counter key
     *
     * @return map of counter key -> counter value
     */
    Map<String, Long> entries();
}
