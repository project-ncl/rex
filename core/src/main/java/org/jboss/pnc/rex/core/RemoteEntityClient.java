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
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Possibly convert to JAX-RS RestClient builder to enable dynamic url
 * @see <a href="https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#_sample_builder_usage">JAX-RS Builder</a>
 */

@Unremovable
@ApplicationScoped
@Slf4j
public class RemoteEntityClient {

    private final TaskController controller;

    private final TaskRegistry taskRegistry;

    private final GenericVertxHttpClient client;

    private final ObjectMapper mapper;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    public RemoteEntityClient(GenericVertxHttpClient client, @WithTransactions TaskController controller, TaskRegistry taskRegistry, ObjectMapper mapper) {
        this.controller = controller;
        this.taskRegistry = taskRegistry;
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


        Object payload = getPayload(requestDefinition, task);

        StopRequest request = StopRequest.builder()
                .payload(payload)
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
                .payload(getPayload(requestDefinition, task))
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

    /**
     * Get payload to send from the Request.
     *
     * If previousTaskNames is not set in the request, use the 'attachment' as payload.
     * If previousTaskNames is set, return a list the results of the tasks mentioned
     * in the previousTaskNames into the payload, and the attachment
     *
     * @param requestDefinition request object
     * @param currentTask task whose payload is being generated
     *
     * @return payload to use
     */
    private Object getPayload(Request requestDefinition, Task currentTask) {

        List<Object> dataToSend = new ArrayList<>();
        List<String> previousTaskNames = currentTask.getPreviousTaskNameResults();

        if (previousTaskNames != null) {

            for (String taskName : previousTaskNames) {

                Task previousTask = taskRegistry.getTask(taskName);

                if (previousTask == null) {
                    // TODO: error checking
                } else if (!previousTask.getState().isFinal()) {
                    // TODO: error checking
                } else if (!previousTask.getCorrelationID().equals(currentTask.getCorrelationID())) {
                    // TODO: make sure the task is from the same graph request
                }

                List<ServerResponse> serverResponses = previousTask.getServerResponses();

                if (serverResponses != null && !serverResponses.isEmpty()) {
                    // add last positive server response to the data to send
                    dataToSend.add(serverResponses.get(serverResponses.size() - 1));
                } else {
                    // just add an empty object?
                    dataToSend.add(new Object());
                }
            }
        }

        dataToSend.add(requestDefinition.getAttachment());

        if (dataToSend.size() == 1) {
            return dataToSend.get(0);
        } else {
            return dataToSend;
        }
    }
}
