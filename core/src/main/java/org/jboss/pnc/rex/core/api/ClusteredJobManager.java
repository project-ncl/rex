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

import org.jboss.pnc.rex.model.ClusteredJobReference;

/**
 * ClusteredJobManager is the facade around managing ClusteredJobs. It handles enlisting and delisting.
 */
public interface ClusteredJobManager {

    /**
     * The method enlists(persists) the supplied job reference instance. The instance must have owner set to the ID of
     * local instance, otherwise an exception is thrown.
     *
     * @param cjob job reference instance
     */
    void enlist(ClusteredJobReference cjob);

    /**
     * The method delists the supplied job reference. The instance MUST OWN the reference, otherwise an exception is
     * thrown.
     *
     * @param id job id
     */
    void delist(String id);

    /**
     * The method returns true is the local instance exists and owns the job reference.
     *
     * @param id job id
     * @return true if local instance owns the job
     */
    boolean isOwned(String id);

    /**
     * The method returns true if the job reference exists in the registry.
     *
     * @param id
     * @return
     */
    boolean exists(String id);
}
