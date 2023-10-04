/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core.endpoints;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.core.GenericVertxHttpClient;
import org.jboss.pnc.rex.core.counter.Counter;
import org.jboss.pnc.rex.core.counter.Running;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.requests.StartRequestWithCallback;
import org.jboss.pnc.rex.model.requests.StopRequestWithCallback;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Path("/test")
public class HttpEndpoint {

    @Inject
    GenericVertxHttpClient client;

    @Inject
    ManagedExecutor executor;

    @Inject
    @Running
    Counter running;

    private final Queue<Long> record = new ConcurrentLinkedQueue<>();
    private final Queue<Object> recordedRequestData = new ConcurrentLinkedQueue<>();

    private boolean shouldRecord = false;

    private final AtomicInteger counter = new AtomicInteger(0);

    @POST
    @Path("/accept")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response accept(String request) {
        record(request);
        return Response.ok().build();
    }

    @POST
    @Path("/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stop(String request) {
        record(request);
        return Response.ok().build();
    }

    @POST
    @Path("/stopAndCallback")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopAndCallback(StopRequestWithCallback request) {
        record(request);
        executor.submit(() -> finishTask(request.getPositiveCallback()));
        return Response.ok().build();
    }

    @POST
    @Path("/acceptAndStart")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptAndStart(StartRequestWithCallback request) {
        record(request);
        executor.submit(() -> finishTask(request.getPositiveCallback()));
        return Response.ok("{\"task\": \"" + request.getPayload() + "\"}").build();
    }

    @POST
    @Path("/acceptAndFail")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptAndFail(StartRequestWithCallback request) {
        record(request);
        executor.submit(() -> finishTask(request.getNegativeCallback()));
        return Response.ok("{\"task\": \"" + request.getPayload() + "\"}").build();
    }

    @POST
    @Path("/425eventuallyOK")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response eventuallyAccept(StartRequestWithCallback request, @QueryParam("fails") int fails) {

        if (fails == counter.get()) {
            executor.submit(() -> finishTask(request.getPositiveCallback()));
            return Response.status(200)
                    .header("Content-Type", "application/json")
                    .entity("{\"you\":\"cool\"}")
                    .build();
        }

        counter.incrementAndGet();
        return Response.status(425)
                .header("Content-Type", "application/json")
                .entity("{\"you\":\"wait\"}")
                .build();
    }

    private void finishTask(Request callback) {
        try {
            Thread.sleep(Duration.between(Instant.now(), Instant.now().plusMillis(20)).toMillis()+10);
        } catch (InterruptedException e) {
            //ignore
        }
        String body = "ALL IS OK";

        List<Header> callbackHeaders = new ArrayList<>();
        callbackHeaders.add(Header.builder().name("Content-Type").value("application/json").build());

        if (callback.getHeaders() != null) {
            for (Request.Header header: callback.getHeaders()) {
                callbackHeaders.add(Header.builder().name(header.getName()).value(header.getValue()).build());
            }
        }

        client.makeRequest(callback.getUri(),
                Method.valueOf(callback.getMethod().toString()),
                callbackHeaders,
                body,
                this::onResponse,
                throwable -> log.error("Couldn't reach local scheduler.", throwable));
    }

    private void onResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() < 400) {
            log.info("Callback to scheduler positive!");
        } else {
            log.warn("Callback to scheduler negative! " + response.body().toString());
        }
    }

    private void record(Object data) {
        if (shouldRecord) {
            record.offer(running.getValue());
            recordedRequestData.offer(data);
        }
    }

    public void startRecordingQueue() {
        record.clear();
        recordedRequestData.clear();
        shouldRecord = true;
    }

    public Collection<Long> stopRecording() {
        shouldRecord = false;
        return record;
    }

    public Collection<Object> getRecordedRequestData() {
        return recordedRequestData;
    }

    public void clearRequestCounter() {
        counter.set(0);
    }

    public int getCount() {
        return counter.get();
    }
}
