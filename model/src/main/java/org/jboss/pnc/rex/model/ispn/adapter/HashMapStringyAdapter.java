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
package org.jboss.pnc.rex.model.ispn.adapter;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ProtoAdapter(HashMap.class)
public class HashMapStringyAdapter {

    @ProtoFactory
    public HashMap<String, String> create(Set<KeyValueString> set) {
        var map = new HashMap<String, String>();
        set.forEach(kv -> map.put(kv.getKey(), kv.getValue()));

        return map;
    }

    @ProtoField(value = 1, collectionImplementation = HashSet.class)
    public Set<KeyValueString> getSet(Map<String,String> map) {
        return map.entrySet()
                .stream()
                .map((entry) -> new KeyValueString(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }
}
