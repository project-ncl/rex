package org.jboss.pnc.rex.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;
import org.jboss.resteasy.spi.Failure;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;

@Slf4j
@Provider
public class AllOtherExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception e) {
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        Response response = null;

        if (e instanceof WebApplicationException) {
            response = ((WebApplicationException) e).getResponse();
            if (e instanceof NotFoundException) {
                return response; // In case of 404 we want to return the empty body.
            }
            log.debug("An exception occurred when processing REST response", e);
        } else if (e instanceof Failure) { // Resteasy support
            Failure failure = ((Failure) e);
            if (failure.getErrorCode() > 0) {
                status = failure.getErrorCode();
            }
            response = failure.getResponse();
            log.debug("An exception occurred when processing REST response", e);
        } else {
            log.error("An exception occurred when processing REST response", e);
        }

        Response.ResponseBuilder builder;

        if (response != null) {
            builder = Response.status(response.getStatus());

            // copy headers
            for (Map.Entry<String, List<Object>> en : response.getMetadata().entrySet()) {
                String headerName = en.getKey();
                List<Object> headerValues = en.getValue();
                for (Object headerValue : headerValues) {
                    builder.header(headerName, headerValue);
                }
            }
        } else {
            builder = Response.status(status);
        }

        builder.entity(new ErrorResponse(e)).type(MediaType.APPLICATION_JSON);
        return null;
    }
}
