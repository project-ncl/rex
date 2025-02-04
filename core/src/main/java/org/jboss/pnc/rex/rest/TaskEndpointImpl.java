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
package org.jboss.pnc.rex.rest;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TaskEndpointImpl implements TaskEndpoint {

    private final TaskProvider taskProvider;

    @Inject
    public TaskEndpointImpl(TaskProvider taskProvider) {
        this.taskProvider = taskProvider;
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public Set<TaskDTO> start(CreateGraphRequest request) {
        return taskProvider.create(request);
    }

    @Override
    public Set<TaskDTO> getAll(TaskFilterParameters filterParameters, List<String> queueFilter) {
        // a small hack to be able to request only 'default' queue which is indexed by null
        if (queueFilter != null && queueFilter.contains("null")) {
            queueFilter = new ArrayList<>(queueFilter);
            queueFilter.remove("null");
            queueFilter.add(null);
        }

        Boolean allFiltersAreFalse = !filterParameters.getFinished() && !filterParameters.getRunning() && !filterParameters.getWaiting();

        //If query is empty return all services
        if (allFiltersAreFalse) {
            return taskProvider.getAll(true,true,true, queueFilter);
        }
        return taskProvider.getAll(
                filterParameters.getWaiting(),
                filterParameters.getRunning(),
                filterParameters.getFinished(),
                queueFilter);
    }

    @Override
    public TaskDTO getSpecific(String taskID) {
        return taskProvider.get(taskID);
    }

    @Override
    public Set<TaskDTO> byCorrelation(String correlationID) {
        return taskProvider.getByCorrelationID(correlationID);
    }

    @Override
    @ApplyFaultTolerance("internal-retry")
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public Response cancel(String taskID) {
        taskProvider.cancel(taskID);

        return Response.accepted().build();
    }
}
