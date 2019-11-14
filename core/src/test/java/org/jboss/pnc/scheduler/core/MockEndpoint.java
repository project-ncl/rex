package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.ServiceContainer;
import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/test")
public class MockEndpoint {
    Logger logger = LoggerFactory.getLogger(MockEndpoint.class);

    @Inject
    ServiceContainer container;

    ExecutorService executor = Executors.newFixedThreadPool(4);

    @Inject
    TransactionManager tm;

    @POST
    @Path("/accept")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response accept(String body){
        logger.debug("payload==" + body);
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

    @POST
    @Path("/acceptAndStart")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response acceptAndStart(String body){
        logger.debug("payload==" + body);
        logger.debug("Endpoint acceptAndStart called");
        executor.submit(() -> retry(10, () -> invokeAccept(body)));
        return Response.ok().build();
    }

    private void invokeAccept(String body) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //shouldn't happen
        }
        System.out.println("Calling accept on: " + body);
        try {
                tm.begin();
                container.getServiceController(ServiceName.parse(body)).accept();
                tm.commit();
        } catch (RollbackException | HeuristicRollbackException | NotSupportedException | HeuristicMixedException | SystemException e) {
            throw new ConcurrentUpdateException("Unexpected error has during committing", e);
        }
    }

    private static void retry(int times, Runnable runnable) {
        for (int i = 0; i < times; i++) {
            try {
                runnable.run();
                break;
            } catch (ConcurrentUpdateException e) {
                System.out.println("retry number: " + i);
            }
        }
        throw new IllegalStateException("Enough is enough!");
    }
}
