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
package org.jboss.pnc.rex.core.api;

/**
 * The interface Task container. Container is a registry and target for installations.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskContainer extends TaskRegistry, TaskTarget {
    /**
     * Initiates 'graceful' shutdown of the container. Currently, not implemented.
     */
    void shutdown();

    /**
     * Gets the name of the container/node
     *
     * @return name of the instance
     */
    String getDeploymentName();

    /**
     * Returns true is container is shutting down. Currently, not implemented.
     *
     * @return boolean
     */
    boolean isShutdown();
}
