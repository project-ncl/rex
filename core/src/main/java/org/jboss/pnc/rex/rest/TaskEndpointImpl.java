/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.facade.api.TaskProvider;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class TaskEndpointImpl implements TaskEndpoint {

    private final TaskProvider taskProvider;

    @Inject
    public TaskEndpointImpl(TaskProvider taskProvider) {
        this.taskProvider = taskProvider;
    }

    @Override
    @ApplyGuard("internal-retry")
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

        Boolean allFiltersAreFalse = !filterParameters.getFinished() && !filterParameters.getRunning()
                && !filterParameters.getWaiting() && !filterParameters.getRollingback();

        //If query is empty return all tasks
        if (allFiltersAreFalse) {
            return taskProvider.getAll(true, true, true, true, queueFilter);
        }
        return taskProvider.getAll(
                filterParameters.getWaiting(),
                filterParameters.getRunning(),
                filterParameters.getFinished(),
                filterParameters.getRollingback(),
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
    @ApplyGuard("internal-retry")
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-app-rex-user", "pnc-users-admin" })
    public Response cancel(String taskID) {
        taskProvider.cancel(taskID);

        return Response.accepted().build();
    }
}
