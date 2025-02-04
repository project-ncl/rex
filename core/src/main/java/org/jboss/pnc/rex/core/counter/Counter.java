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

import jakarta.annotation.Nullable;
import org.infinispan.client.hotrod.VersionedValue;

import java.util.Map;

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
