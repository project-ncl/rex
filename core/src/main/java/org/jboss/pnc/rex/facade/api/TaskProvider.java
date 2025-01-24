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
package org.jboss.pnc.rex.facade.api;

import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.List;
import java.util.Set;

public interface TaskProvider {

    //todo document
    Set<TaskDTO> create(CreateGraphRequest request);
    /**
     * returns all services based on filter
     *
     * @return set of services
     */
    Set<TaskDTO> getAll(boolean waiting, boolean running, boolean finished);

    /**
     * Cancels execution of the service and its dependants
     *
     * @param taskName existing service
     */
    void cancel(String taskName);

    /**
     * Cancels execution of all the tasks in the correlationID
     *
     * @param correlationID the correlationID of the tasks to cancel
     */
    void cancelByCorrelationID(String correlationID);

    /**
     * Returns existing service based on param
     *
     * @param taskName name of existing service
     * @return service entity
     */
    TaskDTO get(String taskName);

    Set<TaskDTO> getByCorrelationID(String correlationID);

    /**
     * Returns all related services
     * (all dependants, all dependencies, dependants of dependencies, dependencies of dependants)
     *
     * @param taskName name of existing service
     * @return set of related services
     */
    List<TaskDTO> getAllRelated(String taskName);

    /**
     * Used for communication with remote entity. Invoked by remote entity by provided callback.
     *
     * @param positive callback is positive or negative
     *          true == remote entity responds that it has finished execution of the service
     *          false == remote entity responds that the service has failed its execution
     */
    void acceptRemoteResponse(String taskName, boolean positive, Object response);
}
