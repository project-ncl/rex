package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.core.counter.Counter;
import org.jboss.pnc.scheduler.core.counter.Running;
import org.jboss.pnc.scheduler.core.endpoints.MockEndpoint;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;
import org.jboss.pnc.scheduler.rest.api.TaskEndpoint;
import org.jboss.pnc.scheduler.rest.parameters.TaskFilterParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.scheduler.common.enums.State.ENQUEUED;
import static org.jboss.pnc.scheduler.core.common.Assertions.waitTillTasksAre;
import static org.jboss.pnc.scheduler.core.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.scheduler.core.common.TestData.getAllParameters;
import static org.jboss.pnc.scheduler.core.common.TestData.getComplexGraph;
import static org.jboss.pnc.scheduler.core.common.TestData.getSingleWithoutStart;

@QuarkusTest
public class QueueTest {

    public static final String EXISTING_KEY = "omg.wtf.whatt";

    @Inject
    TaskContainerImpl container;

    @Inject
    @Running
    Counter running;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    InternalEndpoint internalEndpoint;

    @Inject
    MockEndpoint mockEndpoint;

    @BeforeEach
    void before() {
        running.initialize(0L);
        container.getCache().clear();
    }

    @Test
    void testNoServiceStartsWithMaxBeingZero() {
        internalEndpoint.setConcurrent(0L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.create(graph);

        assertThatThrownBy(() -> waitTillTasksAre(
                State.SUCCESSFUL,
                container,
                1,
                graph.getVertices().keySet().toArray(new String[0]))
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void testComplexStartsWithMaxBeingNonZero() {
        internalEndpoint.setConcurrent(1L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.create(graph);

        waitTillTasksAre(State.SUCCESSFUL, container, graph.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    void testComplexGraphSucceedsAfterChangingMaxToNonZero() throws Exception{
        internalEndpoint.setConcurrent(0L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.create(graph);

        //wait a 1/10 sec
        Thread.sleep(100);
        List<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        assertThat(all)
                .extracting("state", State.class)
                .allMatch((state -> state.isIdle() || state.isQueued()));

        internalEndpoint.setConcurrent(2L);
        waitTillTasksAre(State.SUCCESSFUL, container, graph.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    void testChangingMaxCounterWillTriggerPoke() {
        internalEndpoint.setConcurrent(0L);

        taskEndpoint.create(getSingleWithoutStart(EXISTING_KEY));

        TaskFilterParameters params = getAllParameters();
        List<TaskDTO> tasks = taskEndpoint.getAll(params);
        assertThat(tasks).hasSize(1);

        TaskDTO task = tasks.iterator().next();
        assertThat(task.getName()).isEqualTo(EXISTING_KEY);
        assertThat(task.getState()).isEqualTo(ENQUEUED);

        internalEndpoint.setConcurrent(1L);
        waitTillTasksAre(State.UP, container, container.getTask(EXISTING_KEY));
    }

    @Test
    void testRunningQueue() {
        internalEndpoint.setConcurrent(1L);

        // to make the test deterministic
        int seed = 1000;
        mockEndpoint.startRecordingQueue();

        CreateGraphRequest graph = generateDAG(seed, 2, 10, 5, 10, 0.7F);
        taskEndpoint.create(graph);

        waitTillTasksAre(State.SUCCESSFUL, container, 20, graph.getVertices().keySet().toArray(new String[0]));

        Collection<Long> queueRecords = mockEndpoint.stopRecording();
        assertThat(queueRecords).allMatch(record -> record <=1);
    }
}
