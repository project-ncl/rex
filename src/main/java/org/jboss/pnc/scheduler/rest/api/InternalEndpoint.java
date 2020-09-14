package org.jboss.pnc.scheduler.rest.api;


import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Tag(name = "Internal endpoint")
@Path("/rest/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InternalEndpoint {

    @Path("/{serviceName}/finish")
    @POST
    void finish(@PathParam("serviceName") String serviceName, FinishRequest result);
}
