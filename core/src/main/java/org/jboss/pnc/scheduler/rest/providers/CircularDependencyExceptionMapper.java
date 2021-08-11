package org.jboss.pnc.scheduler.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.scheduler.common.exceptions.CircularDependencyException;
import org.jboss.pnc.scheduler.dto.responses.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class CircularDependencyExceptionMapper implements ExceptionMapper<CircularDependencyException> {
    @Override
    public Response toResponse(CircularDependencyException e) {
        Response.Status status = Response.Status.BAD_REQUEST;
        log.warn("Scheduling request results in circle: " + e, e);
        return Response.status(status)
                .entity(new ErrorResponse(e))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
