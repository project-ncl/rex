/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class TaskMissingExceptionMapper implements ExceptionMapper<TaskMissingException> {
    @Override
    public Response toResponse(TaskMissingException e) {
        Response.Status status = Response.Status.BAD_REQUEST;
        log.warn("Task missing in critical moment: " + e, e);
        return Response.status(status)
                .entity(new ErrorResponse(e, e.getTaskName()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
