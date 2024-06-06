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

import io.quarkus.arc.Unremovable;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.common.exceptions.RequestRetryException;
import org.jboss.pnc.rex.core.mapper.MiniTaskMapper;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.jboss.pnc.rex.common.util.MDCUtils.wrapWithMDC;

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

    public boolean notifyCaller(Transition transition, Task task) {

        if (task.getCallerNotifications() == null) {
            log.warn("NOTIFICATION {}: DISABLED", task.getName());
            return false;
        }

        if (task.getConfiguration() != null && task.getConfiguration().getMdcHeaderKeyMapping() != null) {
            var keys = task.getConfiguration().getMdcHeaderKeyMapping();
            var headers = task.getCallerNotifications().getHeaders().stream().collect(Collectors.toMap(Header::getName, Header::getValue));

            return wrapWithMDC(keys, headers, () -> notifyCallerInternal(transition, task));
        } else {
            return notifyCallerInternal(transition, task);
        }
    }

    private boolean notifyCallerInternal(Transition transition, Task task) {
        Request requestDefinition = task.getCallerNotifications();

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

        AtomicBoolean result = new AtomicBoolean(false);
        client.makeRequest(uri,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, transition, task, result),
                throwable -> onConnectionFailure(throwable, task, result));
        return result.get();
    }

    private void handleResponse(HttpResponse<Buffer> response, Transition transition, Task task, AtomicBoolean result) {
        if (200 <= response.statusCode() && response.statusCode() <= 299) {
            log.debug("NOTIFICATION {}: Successful for transition {} ", task.getName(), transition);

            result.set(true);
        } else if (300 <= response.statusCode() && response.statusCode() <= 499) {
            log.warn("NOTIFICATION {}: Failure while sending notification for transition {}. RESPONSE: {}",
                    task.getName(),
                    transition,
                    response.bodyAsString());

            result.set(false);
        } else {
            // trigger retry
            log.warn("NOTIFICATION {}: System Failure while sending notification for transition {}. RESPONSE: {}",
                    task.getName(),
                    transition,
                    response.bodyAsString());
            throw new RequestRetryException("Retrying");
        }
    }

    private void onConnectionFailure(Throwable exception, Task task, AtomicBoolean result) {
        log.error("NOTIFICATION {}: HTTP call to the Caller failed multiple times.", task, exception);
        // IS THIS A FAIL STATE? SHOULD I THROW EXCEPTION? SHOULD FAILING BE CONFIGURABLE?
        result.set(false);
    }

}
