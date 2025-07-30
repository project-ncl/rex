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

import org.jboss.pnc.rex.common.enums.ResponseFlag;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface TaskProvider {

    //todo document
    Set<TaskDTO> create(CreateGraphRequest request);
    /**
     * returns all tasks based on filter
     *
     * @return set of tasks
     */
    Set<TaskDTO> getAll(boolean waiting, boolean running, boolean finished, boolean rollingback, List<String> queueFilter);

    /**
     * Cancels execution of the task and its dependants
     *
     * @param taskName existing task
     */
    void cancel(String taskName);

    /**
     * Returns existing service based on param
     *
     * @param taskName name of existing task
     * @return task entity
     */
    TaskDTO get(String taskName);

    Set<TaskDTO> getByCorrelationID(String correlationID);

    /**
     * Returns all related services
     * (all dependants, all dependencies, dependants of dependencies, dependencies of dependants)
     *
     * @param taskName name of existing task
     * @return set of related tasks
     */
    List<TaskDTO> getAllRelated(String taskName);

    /**
     * Used for communication with remote entity. Invoked by remote entity by provided callback. Remote entity responds
     * that it has finished execution of the service.
     *
     * @param taskName name of existing task
     * @param rollback whether this response comes from rollback endpoint
     * @param response body of the response
     * @param flags        OPTIONAL flags that slightly modify the behaviour of schedule (for example SKIP_ROLLBACK will
     *                     skip rollback process and fail the Task immediately)
     */
    void positiveRemoteResponse(String taskName, boolean rollback, Object response, Set<ResponseFlag> flags);

    /**
     * Used for communication with remote entity. Invoked by remote entity by provided callback. Remote entity responds
     * that the service has failed its execution.
     *
     * @param taskName     name of existing task
     * @param rollback     whether this response comes from rollback endpoint
     * @param response     body of the response
     * @param flags        OPTIONAL flags that slightly modify the behaviour of schedule (for example SKIP_ROLLBACK will
     *                     skip rollback process and fail the Task immediately)
     */
    void negativeRemoteResponse(String taskName, boolean rollback, Object response, Set<ResponseFlag> flags);

    /**
     * Notifies the task controller that the remotely running task is alive.
     *
     * @param taskName name of existing task
     * @param body request body
     * @param beatTime the earliest time of the received request
     */
    void beat(String taskName, Object body, Instant beatTime);
}
