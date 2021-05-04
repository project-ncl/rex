package org.jboss.pnc.scheduler.core.endpoints;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.scheduler.common.enums.Method;
import org.jboss.pnc.scheduler.core.GenericVertxHttpClient;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.model.requests.StartRequest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

@Slf4j
@Path("/v2/test")
public class HttpEndpoint {

    @Inject
    GenericVertxHttpClient client;

    @Inject
    ManagedExecutor executor;

    @POST
    @Path("/acceptAndStart")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptAndStart(StartRequest request) {
        executor.submit(() -> finishTask(request));
        return Response.ok().build();
    }

    private void finishTask(StartRequest request) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            //ignore
        }
        FinishRequest body = new FinishRequest(true);
        client.makeRequest(URI.create(request.getCallback()),
                Method.POST,
                Collections.emptyList(),
                body,
                this::onResponse);
    }

    private void onResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() < 400) {
            log.info("Positive response!");
        } else {
            log.warn("Negative response!");
        }
    }
}
