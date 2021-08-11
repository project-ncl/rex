package org.jboss.pnc.scheduler.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.scheduler.dto.responses.ErrorResponse;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class ConstraintValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException e) {
        Response.Status status = Response.Status.BAD_REQUEST;
        log.warn("DTO Validation failed: " + e, e);
        return Response.status(status)
                .entity(new ErrorResponse(e))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
