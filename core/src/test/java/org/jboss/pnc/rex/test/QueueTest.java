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
package org.jboss.pnc.rex.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.TaskContainerImpl;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.rex.common.enums.State.ENQUEUED;
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

        Collection<Long> queueRecords = httpEndpoint.stopRecording();
        assertThat(queueRecords).allMatch(record -> record <=1);
    }
}
