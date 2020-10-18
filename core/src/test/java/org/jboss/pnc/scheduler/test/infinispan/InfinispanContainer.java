package org.jboss.pnc.scheduler.test.infinispan;

import org.testcontainers.containers.GenericContainer;

/**
 * Infinispan testcontainers container. Requires docker to be installed.
 *
 * For further Image configuration:
 * @see <a href=https://github.com/infinispan/infinispan-images/blob/master/README.md>Infinispan Image GitHub</a>
 */
public class InfinispanContainer extends GenericContainer<InfinispanContainer> {

    private static final String INFINISPAN_USERNAME = "user";
    private static final String INFINISPAN_PASSWORD = "1234";
    private static final String INFINISPAN_VERSION = "11.0.3.Final";

    public InfinispanContainer(boolean useNative) {
        this("infinispan/server" + (useNative ? "" : "-native" + ":" + INFINISPAN_VERSION));
    }

    public InfinispanContainer(String imageName) {
        super(imageName);
        withExposedPorts(11222);
        addCredentials();
    }

    private void addCredentials() {
        withEnv("USER", INFINISPAN_USERNAME);
        withEnv("PASS", INFINISPAN_PASSWORD);
    }

    public String getIPAddress() {
        return getContainerIpAddress();
    }


}
