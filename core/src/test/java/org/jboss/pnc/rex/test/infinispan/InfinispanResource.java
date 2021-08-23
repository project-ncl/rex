package org.jboss.pnc.rex.test.infinispan;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

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
