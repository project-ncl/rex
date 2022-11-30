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
package org.jboss.pnc.rex.test.infinispan;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * @deprecated Infinispan Dev-services are used instead
 */
@Deprecated
public class InfinispanResource implements QuarkusTestResourceLifecycleManager {

    public final InfinispanContainer infinispan = new InfinispanContainer(false);

    @Override
    public Map<String, String> start() {
        infinispan.start();
        return Map.of("quarkus.infinispan-client.server-list", infinispan.getIPAddress());
    }

    @Override
    public void stop() {
        infinispan.stop();
    }
}
