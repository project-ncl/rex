/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.exceptions.QueueMissingException;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;

@Slf4j
@Provider
public class QueueMissingExceptionMapper implements ExceptionMapper<QueueMissingException>  {

    @Override
    public Response toResponse(QueueMissingException e) {
        Response.Status status = Response.Status.NOT_FOUND;
        log.info("Queue with name {} was not found.", e.getQueueName());
        return Response.status(status)
                .entity(new ErrorResponse(e, e.getQueueName()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
