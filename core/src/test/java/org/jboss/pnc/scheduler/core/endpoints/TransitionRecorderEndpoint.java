package org.jboss.pnc.scheduler.core.endpoints;

import io.vertx.core.impl.ConcurrentHashSet;
import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.model.requests.NotificationRequest;

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
