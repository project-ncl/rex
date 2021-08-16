package org.jboss.pnc.rex.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

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

    private final ObjectMapper mapper;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    public RemoteEntityClient(GenericVertxHttpClient client, @WithTransactions TaskController controller, ObjectMapper mapper) {
        this.controller = controller;
        this.client = client;
        this.mapper = mapper;
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
                response -> handleResponse(response, task),
                throwable -> handleConnectionFailure(throwable, task));
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
                response -> handleResponse(response, task),
                throwable -> handleConnectionFailure(throwable, task));
    }

    private void handleResponse(HttpResponse<Buffer> response, Task task) {
        if (200 <= response.statusCode() && response.statusCode() <= 299) {
            log.info("RESPONSE {}: Got positive response.", task.getName());
            controller.accept(task.getName(), parseBody(response));
        } else {
            log.info("RESPONSE {}: Got negative response. (STATUS CODE: {})", task.getName(), response.statusCode());
            controller.fail(task.getName(), parseBody(response));
        }
    }

    private void handleConnectionFailure(Throwable exception, Task task) {
        log.error("ERROR " + task.getName() + ": Couldn't reach the remote entity.", exception);
        // format of the simulated "response" could be better (mainly not String)
        Uni.createFrom().voidItem()
            .onItem().invoke(() -> controller.fail(task.getName(), "Remote entity failed to respond. Exception: " + exception.toString()))
            .onFailure().retry().atMost(5)
            .onFailure().invoke((throwable) -> log.error("ERROR: Couldn't commit transaction. Data corruption is possible.", throwable))
            .onFailure().recoverWithNull()
            .await().indefinitely();
    }

    private Object parseBody(HttpResponse<Buffer> response) {
        String body = response.bodyAsString();
        Object objectResponse = null;
        if (body != null) {
            try {
                objectResponse = mapper.readValue(body, Object.class);
            } catch (JsonProcessingException ignored) {
                log.warn("Response(statusCode: {}) could not be parsed. Response: {}", response.statusCode(), response.bodyAsString());
            }
        }
        return objectResponse;
    }
}
