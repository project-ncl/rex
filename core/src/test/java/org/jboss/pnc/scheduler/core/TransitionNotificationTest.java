package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.common.TestDataV2;
import org.jboss.pnc.scheduler.core.counter.Counter;
import org.jboss.pnc.scheduler.core.counter.Running;
import org.jboss.pnc.scheduler.core.endpoints.TransitionRecorderEndpoint;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;
import org.jboss.pnc.scheduler.rest.api.TaskEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.scheduler.common.enums.Transition.ENQUEUED_to_STARTING;
import static org.jboss.pnc.scheduler.common.enums.Transition.NEW_to_ENQUEUED;
import static org.jboss.pnc.scheduler.common.enums.Transition.NEW_to_WAITING;
import static org.jboss.pnc.scheduler.common.enums.Transition.STARTING_to_UP;
import static org.jboss.pnc.scheduler.common.enums.Transition.STOPPING_to_STOPPED;
import static org.jboss.pnc.scheduler.common.enums.Transition.UP_to_STOPPING;
import static org.jboss.pnc.scheduler.common.enums.Transition.UP_to_SUCCESSFUL;
import static org.jboss.pnc.scheduler.common.enums.Transition.WAITING_to_ENQUEUED;
import static org.jboss.pnc.scheduler.common.enums.Transition.WAITING_to_STOPPED;
import static org.jboss.pnc.scheduler.core.common.Assertions.waitTillServicesAre;

@QuarkusTest
public class TransitionNotificationTest {

    @Inject
    TaskContainerImpl container;

    @Inject
    TaskEndpoint endpoint;

    @Inject
    InternalEndpoint internalEndpoint;

    @Inject
    @Running
    Counter running;

    @Inject
    TransitionRecorderEndpoint recorderEndpoint;

    @BeforeEach
    void before() {
        running.initialize(0L);
        internalEndpoint.setConcurrent(10L);
        recorderEndpoint.flush();
        container.getCache().clear();
    }

    @Test
    void testNotifications() throws InterruptedException {
        CreateGraphRequest request = TestDataV2.getComplexGraph(true, true);
        endpoint.create(request);
        waitTillServicesAre(State.SUCCESSFUL, container, request.getVertices().keySet().toArray(new String[0]));
        
        Thread.sleep(100);
        Map<String, Set<Transition>> records = recorderEndpoint.getRecords();
        assertThat(records.keySet()).containsExactlyInAnyOrderElementsOf(request.getVertices().keySet());
        assertThat(records.get("a")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("b")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("c")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("d")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("e")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("f")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("g")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("h")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("i")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
        assertThat(records.get("j")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_SUCCESSFUL));
    }

    @Test
    void testNotificationOnCancel() throws InterruptedException {
        CreateGraphRequest request = TestDataV2.getComplexGraphWithoutEnd(true, true);
        endpoint.create(request);
        waitTillServicesAre(State.UP, container, "a", "b");

        endpoint.cancel("a");
        endpoint.cancel("b");

        waitTillServicesAre(State.STOPPED, container, request.getVertices().keySet().toArray(new String[0]));

        Thread.sleep(100);
        Map<String, Set<Transition>> records = recorderEndpoint.getRecords();
        assertThat(records.keySet()).containsExactlyInAnyOrderElementsOf(request.getVertices().keySet());
        assertThat(records.get("a")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_STOPPING, STOPPING_to_STOPPED));
        assertThat(records.get("b")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_ENQUEUED, ENQUEUED_to_STARTING, STARTING_to_UP, UP_to_STOPPING, STOPPING_to_STOPPED));
        assertThat(records.get("c")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("d")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("e")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("f")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("g")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("h")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("i")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
        assertThat(records.get("j")).containsExactlyInAnyOrderElementsOf(Set.of(NEW_to_WAITING, WAITING_to_STOPPED));
    }
}
