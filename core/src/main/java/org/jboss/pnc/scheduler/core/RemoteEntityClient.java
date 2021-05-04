package org.jboss.pnc.scheduler.core;

import io.quarkus.arc.Unremovable;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.delegates.WithTransactions;
import org.jboss.pnc.scheduler.model.Request;
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

@Unremovable
@ApplicationScoped
@Slf4j
public class RemoteEntityClient {

    private final TaskController controller;

    private final GenericVertxHttpClient client;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    public RemoteEntityClient(GenericVertxHttpClient client, @WithTransactions TaskController controller) {
        this.controller = controller;
        this.client = client;
    }

    public void stopJob(Task task) {
        Request requestDefinition = task.getRemoteCancel();

        URI url;
        try {
            url = new URI(requestDefinition.getUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("remoteCancel.url is not a valid URL for task with name " +
                    task.getName(), e);
        }

        StopRequest request = StopRequest.builder()
                .payload(requestDefinition.getAttachment())
                .callback(baseUrl + "/rest/internal/"+ task.getName() + "/finish")
                .build();

        client.makeRequest(url,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, task));
    }

    public void startJob(Task task) {
        Request requestDefinition = task.getRemoteStart();

        URI uri;
        try {
            uri = new URI(requestDefinition.getUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("remoteStart.url is not a valid URL for task with name " +
                    task.getName(), e);
        }

        StartRequest request = StartRequest.builder()
                .payload(requestDefinition.getAttachment())
                .callback(baseUrl + "/rest/internal/"+ task.getName() + "/finish")
                .build();

        client.makeRequest(uri,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, task));
    }

    private void handleResponse(HttpResponse<Buffer> response, Task task) {
        if (200 <= response.statusCode() && response.statusCode() <= 299) {
            log.info("Got positive response on " + task.getName());
            controller.accept(task.getName());
        } else {
            log.info("Got negative response on " + task.getName());
            controller.fail(task.getName());
        }
    }
}
