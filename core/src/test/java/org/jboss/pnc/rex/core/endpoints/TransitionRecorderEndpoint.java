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

import io.vertx.core.impl.ConcurrentHashSet;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Path("/transition")
public class TransitionRecorderEndpoint {

    private final Map<String, Set<Transition>> recorder = new ConcurrentHashMap<>();

    @POST
    @Path("/record")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response record(NotificationRequest request) {
        String taskName = request.getTask().getName();
        if (!recorder.containsKey(taskName)) {
            recorder.put(taskName, new ConcurrentHashSet<>());
        }
        Optional<Transition> transition = Arrays.stream(Transition.values())
                .filter(t -> t.getBefore() == request.getBefore()
                        && t.getAfter() == request.getAfter())
                .findFirst();
        if (transition.isPresent()) {
            recorder.get(taskName).add(transition.get());
            return Response.ok().build();
        }
        return Response.serverError().build();
    }

    public void flush() {
        recorder.clear();
    }

    public Map<String, Set<Transition>> getRecords() {
        return Collections.unmodifiableMap(recorder);
    }
}
