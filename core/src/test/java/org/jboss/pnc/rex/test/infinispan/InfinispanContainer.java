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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Infinispan testcontainers container. Requires docker to be installed.
 *
 * For further Image configuration:
 * @see <a href=https://github.com/infinispan/infinispan-images/blob/master/README.md>Infinispan Image GitHub</a>
 * @deprecated Infinispan Dev-services are used instead
 */
@Deprecated
public class InfinispanContainer extends GenericContainer<InfinispanContainer> {

    private static final String INFINISPAN_USERNAME = "admin";
    private static final String INFINISPAN_PASSWORD = "password";
    private static final String INFINISPAN_VERSION = "13.0.5.Final-2";

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
        return getHost() +':'+ getMappedPort(11222);
    }


}
