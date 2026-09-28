/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class TaskConflictExceptionMapper implements ExceptionMapper<TaskConflictException> {
    @Override
    public Response toResponse(TaskConflictException e) {
        Response.Status status = Response.Status.CONFLICT;
        log.warn("Task conflict found: " + e, e);
        return Response.status(status)
                .entity(new ErrorResponse(e, e.getConflictId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
