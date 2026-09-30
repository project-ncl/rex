/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.endpoints;

import io.vertx.core.impl.ConcurrentHashSet;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.model.TransitionTime;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Path("/transition")
public class TransitionRecorderEndpoint {

    private final Map<String, Set<TransitionTime>> recorder = new ConcurrentHashMap<>();

    @POST
    @Path("/record")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response record(NotificationRequest request) {
        Instant timestamp = Instant.now();

        String taskName = request.getTask().getName();
        if (!recorder.containsKey(taskName)) {
            recorder.put(taskName, new ConcurrentHashSet<>());
        }
        Optional<Transition> transition = Arrays.stream(Transition.values())
                .filter(t -> t.getBefore() == request.getBefore()
                        && t.getAfter() == request.getAfter())
                .findFirst();
        if (transition.isPresent()) {
            recorder.get(taskName).add(TransitionTime.builder()
                    .time(timestamp)
                    .transition(transition.get())
                    .build());
            return Response.ok().build();
        }
        return Response.serverError().build();
    }

    @POST
    @Path("/fail")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response beBad(NotificationRequest request) {
        if (request.getAfter().isFinal()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().build();
    }

    public void flush() {
        recorder.clear();
    }

    public Map<String, Set<Transition>> getRecords() {
        return Collections.unmodifiableMap(
                recorder.entrySet().stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .map(TransitionTime::getTransition)
                                        .collect(Collectors.toSet()))
                        )
        );
    }

    public Map<String, Set<TransitionTime>> getRecordsWithTimestamps() {
        return Collections.unmodifiableMap(recorder);
    }
}
