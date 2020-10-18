package org.jboss.pnc.scheduler.test.infinispan;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

public class InfinispanResource implements QuarkusTestResourceLifecycleManager {

    private final InfinispanContainer infinispan = new InfinispanContainer(true);

    @Override
    public Map<String, String> start() {
        infinispan.start();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        infinispan.close();
    }
}
