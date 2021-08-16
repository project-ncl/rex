package org.jboss.pnc.rex.core;

import io.quarkus.arc.Unremovable;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.mapper.MiniTaskMapper;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;

@Unremovable
@ApplicationScoped
@Slf4j
public class CallerNotificationClient {

    private final MiniTaskMapper miniMapper;

    private final GenericVertxHttpClient client;

    public CallerNotificationClient(MiniTaskMapper miniMapper, GenericVertxHttpClient client) {
        this.miniMapper = miniMapper;
        this.client = client;
    }

    public void notifyCaller(Transition transition, Task task) {
        Request requestDefinition = task.getCallerNotifications();

        if (requestDefinition == null) {
            log.warn("NOTIFICATION {}: DISABLED", task.getName());
            return;
        }

        URI uri;
        try {
            uri = new URI(requestDefinition.getUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Url for notifications is not a valid URL for task with name "
                    + task.getName(), e);
        }

        NotificationRequest request = NotificationRequest.builder()
                .before(transition.getBefore())
                .after(transition.getAfter())
                .attachment(requestDefinition.getAttachment())
                .task(miniMapper.minimize(task))
                .build();

        log.info("NOTIFICATION {}: {} transition. Sending notification. REQUEST: {}.",
                task.getName(),
                transition,
                request.toString());

        client.makeRequest(uri,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, transition, task),
                throwable -> onConnectionFailure(throwable, task));
    }

    private void handleResponse(HttpResponse<Buffer> response, Transition transition, Task task) {
        if (200 <= response.statusCode() && response.statusCode() <= 299) {
            log.debug("NOTIFICATION {}: Successful for transition {} ", task.getName(), transition);
        } else {
            log.warn("NOTIFICATION {}: Failure while sending notification for transition {}. RESPONSE: {}",
                    task.getName(),
                    transition,
                    response.bodyAsString());
        }
    }

    private void onConnectionFailure(Throwable exception, Task task) {
       log.error("NOTIFICATION {}: HTTP call to the Caller failed multiple times.", task, exception);
       // IS THIS A FAIL STATE? SHOULD I THROW EXCEPTION? SHOULD FAILING BE CONFIGURABLE?
    }
}
