package org.jboss.pnc.scheduler.core;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.delegates.WithTransactions;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.model.requests.StartRequest;
import org.jboss.pnc.scheduler.model.requests.StopRequest;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * TODO: Possibly convert to JAX-RS RestClient builder to enable dynamic url
 * @see <a href="https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#_sample_builder_usage">JAX-RS Builder</a>
 */
@ApplicationScoped
@Slf4j
public class RemoteEntityClient {
    private final TaskController controller;
    private final WebClient client;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    public RemoteEntityClient(Vertx vertx, @WithTransactions TaskController controller) {
        this.controller = controller;
        this.client = WebClient.create(vertx);
    }

    public void stopJob(Task task) {
        URI url;
        try {
            url = new URI(task.getRemoteEndpoints().getStopUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("StopUrl is not a valid URL for task with name " + task.getName(), e);
        }

        StopRequest request = StopRequest.builder()
                .payload(task.getPayload())
                .callback(baseUrl + "/rest/internal/"+ task.getName() + "/finish")
                .build();

        makeRequest(task, url, request);
    }

    public void startJob(Task task) {
        URI url;
        try {
            url = new URI(task.getRemoteEndpoints().getStartUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("StartUrl is not a valid URL for task with name " + task.getName(), e);
        }

        StartRequest request = StartRequest.builder()
                .payload(task.getPayload())
                .callback(baseUrl + "/rest/internal/"+ task.getName() + "/finish")
                .build();

        makeRequest(task, url, request);
    }

    private void makeRequest(Task task, URI remoteEndpoint, Object requestBody) {
        client.post(remoteEndpoint.getPort(), remoteEndpoint.getHost(), remoteEndpoint.getPath())
                .sendJson(requestBody)
                .onItem().transformToUni(i ->
                    Uni.createFrom()
                            .item(i).onItem().invoke((resp) -> handleResponse(resp, task))
                            .onFailure().retry().atMost(5))
                .onFailure().retry().atMost(5)
                .await().indefinitely();
    }

    private void handleResponse(HttpResponse<Buffer> response, Task task) {
        if (response.statusCode() < 299) {
            log.info("Got positive response on " + task.getName());
            controller.accept(task.getName());
        } else {
            log.info("Got negative response on " + task.getName());
            controller.fail(task.getName());
        }
    }
}
