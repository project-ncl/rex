package org.jboss.pnc.rex.test.infinispan;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Infinispan testcontainers container. Requires docker to be installed.
 *
 * For further Image configuration:
 * @see <a href=https://github.com/infinispan/infinispan-images/blob/master/README.md>Infinispan Image GitHub</a>
 */
public class InfinispanContainer extends GenericContainer<InfinispanContainer> {

    private static final String INFINISPAN_USERNAME = "user";
    private static final String INFINISPAN_PASSWORD = "a1234";
    private static final String INFINISPAN_VERSION = "12.0.2.Final";

    public InfinispanContainer(boolean useNative) {
        this("infinispan/server" + (useNative ? "-native" + ":" + INFINISPAN_VERSION : ":" + INFINISPAN_VERSION));
    }

    public InfinispanContainer(String imageName) {
        super(imageName);
        withExposedPorts(11222);
        addCredentials();
        waitingFor(Wait.forLogMessage(".*ISPN080001.*", 1));
    }

    private void addCredentials() {
        withEnv("USER", INFINISPAN_USERNAME);
        withEnv("PASS", INFINISPAN_PASSWORD);
    }

    public String getIPAddress() {
        getMappedPort(11222);
        return getContainerIpAddress() +':'+ getMappedPort(11222);
    }


}
