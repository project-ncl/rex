/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.rex.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.TaskContainerImpl;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.rex.common.enums.State.ENQUEUED;
import static org.jboss.pnc.rex.common.enums.State.WAITING;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAre;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.test.common.TestData.getAllParameters;
import static org.jboss.pnc.rex.test.common.TestData.getComplexGraph;
import static org.jboss.pnc.rex.test.common.TestData.getSingleWithoutStart;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class QueueTest extends AbstractTest {

    public static final String EXISTING_KEY = "omg.wtf.whatt";

    @Inject
    TaskContainerImpl container;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    HttpEndpoint httpEndpoint;

    @Inject
    QueueEndpoint queue;

    @Test
    void testNoServiceStartsWithMaxBeingZero() {
        queue.setConcurrent(0L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.start(graph);

        assertThatThrownBy(() -> waitTillTasksAre(
                State.SUCCESSFUL,
                container,
                1,
                graph.getVertices().keySet().toArray(new String[0]))
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void testComplexStartsWithMaxBeingNonZero() {
        queue.setConcurrent(1L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.start(graph);

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    void testComplexGraphSucceedsAfterChangingMaxToNonZero() throws Exception{
        queue.setConcurrent(0L);
        CreateGraphRequest graph = getComplexGraph(true);
        taskEndpoint.start(graph);

        //wait a 1/10 sec
        Thread.sleep(100);
        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        assertThat(all)
                .extracting("state", State.class)
                .allMatch((state -> state.isIdle() || state.isQueued()));

        queue.setConcurrent(2L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    void testChangingMaxCounterWillTriggerPoke() {
        queue.setConcurrent(0L);

        taskEndpoint.start(getSingleWithoutStart(EXISTING_KEY));

        TaskFilterParameters params = getAllParameters();
        Set<TaskDTO> tasks = taskEndpoint.getAll(params);
        assertThat(tasks).hasSize(1);

        TaskDTO task = tasks.iterator().next();
        assertThat(task.getName()).isEqualTo(EXISTING_KEY);
        assertThat(task.getState()).isEqualTo(ENQUEUED);

        queue.setConcurrent(1L);
        waitTillTasksAre(State.UP, container, container.getTask(EXISTING_KEY));
    }

    @Test
    void testRunningQueue() {
        queue.setConcurrent(1L);

        // to make the test deterministic
        int seed = 1000;
        httpEndpoint.startRecordingQueue();

        CreateGraphRequest graph = generateDAG(seed, 2, 10, 5, 10, 0.7F);
        taskEndpoint.start(graph);

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));

        Map<String, ? extends Collection<Long>> queueRecords = httpEndpoint.stopRecording();
        assertThat(queueRecords.get(null)).allMatch(record -> record <= 1);
    }

    @Test
    void testSetNamedQueue() {
        // with
        queue.setConcurrentNamed("named", 1000L);
        LongResponse named = queue.getConcurrentNamed("named");

        assertThat(named).isNotNull();
        assertThat(named.getNumber()).isEqualTo(1000L);
    }

    @Test
    void testTasksHaveDefinedNamedQueue() throws InterruptedException {
        // with
        final String NAMED_QUEUE = "named";
        queue.setConcurrentNamed(NAMED_QUEUE, 1000L);

        CreateGraphRequest graph = getComplexGraph(true);
        graph.queue = NAMED_QUEUE;
        graph.getVertices().values().forEach(task -> task.controllerMode = Mode.IDLE);

        // when
        taskEndpoint.start(graph);
        Thread.sleep(100);

        // then
        var tasks = taskEndpoint.getAll(getAllParameters());
        assertThat(tasks).hasSize(10);
        assertThat(tasks).extracting("queue", String.class)
                .doesNotContainNull()
                .allMatch(NAMED_QUEUE::equals);

    }

    @Test
    void testTasksDoNotStartWithNamedQueue() throws InterruptedException {
        // with
        queue.setConcurrent(1000L);

        String NAMED_QUEUE = "named";
        queue.setConcurrentNamed(NAMED_QUEUE, 0L);
        CreateGraphRequest graph = getComplexGraph(true);
        graph.queue = NAMED_QUEUE;

        // when
        taskEndpoint.start(graph);
        Thread.sleep(100);

        // then
        var tasks = taskEndpoint.getAll(getAllParameters());
        assertThat(tasks).hasSize(10);
        assertThat(tasks).extracting("queue", String.class)
                .doesNotContainNull()
                .allMatch(NAMED_QUEUE::equals);

        assertThat(tasks).extracting("state", State.class)
                .filteredOn(State::isQueued)
                .hasSize(2)
                .allMatch(ENQUEUED::equals);
        assertThat(tasks).extracting("state", State.class)
                .filteredOn(State::isIdle)
                .hasSize(8)
                .allMatch(WAITING::equals);

    }

    @Test
    void testTasksCompleteWithNamedQueue() throws InterruptedException {
        queue.setConcurrent(0L); // make sure default queue is not affecting

        // with
        String NAMED_QUEUE = "named";
        queue.setConcurrentNamed(NAMED_QUEUE, 1000L);

        CreateGraphRequest graph = getComplexGraph(true);
        graph.queue = NAMED_QUEUE;

        // when
        httpEndpoint.startRecordingQueue();
        httpEndpoint.additionallyRecord(NAMED_QUEUE);

        taskEndpoint.start(graph);
        Thread.sleep(100);
        var tasks = taskEndpoint.getAll(getAllParameters());


        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
        Thread.sleep(100);

        // then
        var queueRecords = httpEndpoint.stopRecording();
        assertThat(queueRecords).containsKey(NAMED_QUEUE);
        assertThat(queueRecords.get(NAMED_QUEUE)).allMatch(record -> record >= 1);
    }

    @Test
    void testTasksFinishAfterChangingMaxValue() throws InterruptedException {
        queue.setConcurrent(0L); // make sure default queue is not affecting

        // with
        String NAMED_QUEUE = "named";
        queue.setConcurrentNamed(NAMED_QUEUE, 0L);

        CreateGraphRequest graph = getComplexGraph(true);
        graph.queue = NAMED_QUEUE;

        // when
        httpEndpoint.startRecordingQueue();
        httpEndpoint.additionallyRecord(NAMED_QUEUE);

        taskEndpoint.start(graph);
        Thread.sleep(100);
        var tasks = taskEndpoint.getAll(getAllParameters());
        assertThat(tasks).hasSize(10)
                .allMatch((task) -> task.getState().isIdle() || task.getState().isQueued());

        queue.setConcurrentNamed(NAMED_QUEUE, 1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
        Thread.sleep(100);

        // then
        var queueRecords = httpEndpoint.stopRecording();
        assertThat(queueRecords).containsKey(NAMED_QUEUE);
        assertThat(queueRecords.get(NAMED_QUEUE)).hasSize(10).allMatch(record -> record <= 1);
    }

    @Test
    void testNamedAndDefaultQueuesInteraction() throws InterruptedException {
        String NAMED_QUEUE = "named";
        // with
        queue.setConcurrent(0L);
        queue.setConcurrentNamed(NAMED_QUEUE, 0L);

        var taskA = TestData.getMockTaskWithStart("a", Mode.ACTIVE);
        taskA.queue = null;
        var taskB = TestData.getMockTaskWithStart("b", Mode.ACTIVE);
        taskB.queue = NAMED_QUEUE;

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source("b").target("a").build()) // 'b' depends on 'a'
                .vertex("a", taskA)
                .vertex("b", taskB)
                .build();

        taskEndpoint.start(graph);
        Thread.sleep(100);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        assertThat(all).hasSize(2);
        Map<String, TaskDTO> tasks = all.stream().collect(toMap(TaskDTO::getName, Function.identity()));
        assertThat(tasks.get("a")).extracting(TaskDTO::getState).isEqualTo(State.ENQUEUED);
        assertThat(tasks.get("b")).extracting(TaskDTO::getState).isEqualTo(State.WAITING);

        // should start 'a' that's on default queue but not 'b'
        queue.setConcurrent(1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "a");

        all = taskEndpoint.getAll(getAllParameters());

        assertThat(all).hasSize(2);
        tasks = all.stream().collect(toMap(TaskDTO::getName, Function.identity()));
        assertThat(tasks.get("a")).extracting(TaskDTO::getState).isEqualTo(State.SUCCESSFUL);
        assertThat(tasks.get("b")).extracting(TaskDTO::getState).isEqualTo(State.ENQUEUED);

        queue.setConcurrentNamed(NAMED_QUEUE, 1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "b");
    }

    @Test
    void testNamedQueuesInteraction() throws InterruptedException {
        String NAMED_QUEUE = "named";
        String NAMED_QUEUE_2 = "named-2";
        // with
        queue.setConcurrentNamed(NAMED_QUEUE, 0L);
        queue.setConcurrentNamed(NAMED_QUEUE_2, 0L);

        var taskA = TestData.getMockTaskWithStart("a", Mode.ACTIVE);
        taskA.queue = NAMED_QUEUE;
        var taskB = TestData.getMockTaskWithStart("b", Mode.ACTIVE);
        taskB.queue = NAMED_QUEUE_2;

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source("b").target("a").build()) // 'b' depends on 'a'
                .vertex("a", taskA)
                .vertex("b", taskB)
                .build();

        taskEndpoint.start(graph);
        Thread.sleep(100);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        assertThat(all).hasSize(2);
        Map<String, TaskDTO> tasks = all.stream().collect(toMap(TaskDTO::getName, Function.identity()));
        assertThat(tasks.get("a")).extracting(TaskDTO::getState).isEqualTo(State.ENQUEUED);
        assertThat(tasks.get("b")).extracting(TaskDTO::getState).isEqualTo(State.WAITING);

        // should start 'a' that's on default queue but not 'b'
        queue.setConcurrentNamed(NAMED_QUEUE, 1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "a");
        Thread.sleep(100);

        all = taskEndpoint.getAll(getAllParameters());

        assertThat(all).hasSize(2);
        tasks = all.stream().collect(toMap(TaskDTO::getName, Function.identity()));
        assertThat(tasks.get("a")).extracting(TaskDTO::getState).isEqualTo(State.SUCCESSFUL);
        assertThat(tasks.get("b")).extracting(TaskDTO::getState).isEqualTo(State.ENQUEUED);

        queue.setConcurrentNamed(NAMED_QUEUE_2, 1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "b");
    }

    @Test
    void verifyStartWithDifferentNamedQueuesStartingAtTheSameTime() throws InterruptedException {
        String NAMED_QUEUE = "named";
        String NAMED_QUEUE_2 = "named-2";

        // with
        queue.setConcurrent(0L);
        queue.setConcurrentNamed(NAMED_QUEUE, 1000L);
        queue.setConcurrentNamed(NAMED_QUEUE_2, 1000L);

        var taskA = TestData.getMockTaskWithStart("a", Mode.ACTIVE);
        taskA.queue = null;
        var taskB = TestData.getMockTaskWithStart("b", Mode.ACTIVE);
        taskB.queue = NAMED_QUEUE;
        var taskC = TestData.getMockTaskWithStart("c", Mode.ACTIVE);
        taskB.queue = NAMED_QUEUE_2;

        CreateGraphRequest graph = CreateGraphRequest.builder()
                .edge(EdgeDTO.builder().source("b").target("a").build()) // 'b' depends on 'a'
                .edge(EdgeDTO.builder().source("c").target("a").build()) // 'c' depends on 'a'
                .vertex("a", taskA)
                .vertex("b", taskB)
                .vertex("c", taskC)
                .build();

        taskEndpoint.start(graph);
        Thread.sleep(100);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());
        assertThat(all).hasSize(3);
        Map<String, TaskDTO> tasks = all.stream().collect(toMap(TaskDTO::getName, Function.identity()));
        assertThat(tasks.get("a")).extracting(TaskDTO::getState).isEqualTo(State.ENQUEUED);
        assertThat(tasks.get("b")).extracting(TaskDTO::getState).isEqualTo(State.WAITING);
        assertThat(tasks.get("c")).extracting(TaskDTO::getState).isEqualTo(State.WAITING);

        // should start 'a' that's on default queue and queue 'b' and 'c' at the same time in the same PokeQueueJob
        queue.setConcurrent(1L);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "a", "b", "c");
    }

    @Test
    void testComplexWithNamedQueues() throws InterruptedException {
        // with
        String NAMED_QUEUE = "named";
        String NAMED_QUEUE_2 = "named-2";

        queue.setConcurrent(10L);
        queue.setConcurrentNamed(NAMED_QUEUE, 10L);
        queue.setConcurrentNamed(NAMED_QUEUE_2, 10L);

        CreateGraphRequest graph = getComplexGraph(true);

        graph.getVertices().get("a").queue = null;
        graph.getVertices().get("b").queue = NAMED_QUEUE_2;
        graph.getVertices().get("c").queue = NAMED_QUEUE;
        graph.getVertices().get("d").queue = NAMED_QUEUE_2;
        graph.getVertices().get("e").queue = NAMED_QUEUE;
        graph.getVertices().get("f").queue = null;
        graph.getVertices().get("g").queue = null;
        graph.getVertices().get("h").queue = NAMED_QUEUE_2;
        graph.getVertices().get("i").queue = NAMED_QUEUE_2;
        graph.getVertices().get("j").queue = NAMED_QUEUE;

        // when
        taskEndpoint.start(graph);

        //then
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, graph.getVertices().keySet().toArray(new String[0]));
    }
}
