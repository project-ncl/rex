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
package org.jboss.pnc.rex.core.api;

/**
 * Interface for communicating/messaging dependents (tasks that depend on you).
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface DependentMessenger {
    /**
     * Notify this dependent that it's dependency has succeeded.
     * @param name name of the dependent
     */
    void dependencySucceeded(String name);

    /**
     * Notify this dependent that it's dependency has stopped.
     * @param name name of the dependent
     */
    void dependencyStopped(String name);

    /**
     * Notify this dependent that it's dependency has been cancelled.
     * @param name name of the dependent
     */
    void dependencyCancelled(String name);
}
