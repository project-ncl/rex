package org.jboss.pnc.scheduler.rest.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.dto.responses.LongResponse;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Tag(name = "Internal endpoint")
@Path("/rest/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InternalEndpoint {

    @Path("/{serviceName}/finish")
    @POST
    void finish(@PathParam("serviceName") String serviceName, FinishRequest result);

    @Path("/options/concurrency")
    @POST
    void setConcurrent(@QueryParam("amount") @NotNull Long amount);

    @Path("/options/concurrency")
    @GET
    LongResponse getConcurrent();
}
