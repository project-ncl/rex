package org.jboss.pnc.scheduler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test")
public class MockEndpoint {
    Logger logger = LoggerFactory.getLogger(MockEndpoint.class);

    @POST
    @Path("/accept")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response accept(String body){
        logger.debug(body);
        logger.debug("HELLO GOTTT ITTT");
        return Response.ok().build();
    }

    @POST
    @Path("/stop")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response stop(String string){
        logger.debug("HELLO GOTTT");
        return Response.ok().build();
    }
}
