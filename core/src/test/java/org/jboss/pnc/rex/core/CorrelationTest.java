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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.core.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.core.common.TestData.getAllParameters;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class CorrelationTest {

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    TaskContainerImpl container;

    @Inject
    QueueEndpoint queue;

    @BeforeEach
    void before() {
        // don't actually start tasks because they get deleted after finishing
        queue.setConcurrent(0L);

        // clear ISPN
        container.getCache().clear();
        container.getConstraintCache().clear();
    }

    @Test
    void testAllTasksGetCorrelated() {
        String correlationID = "heavy-metal";

        CreateGraphRequest request = generateDAG(1000,2, 10, 5, 10, 0.7F)
                .toBuilder()
                .correlationID(correlationID)
                .build();

        Set<TaskDTO> start = taskEndpoint.start(request);

        assertThat(start).extracting(task -> task.correlationID).containsOnly(correlationID);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());

        assertThat(all)
                .isNotEmpty()
                .extracting(task -> task.correlationID)
                .containsOnly(correlationID);
    }

    @Test
    void testCorrelationGetAllEndpoint() throws InterruptedException {
        String correlationID = "nu-metal";

        CreateGraphRequest request = generateDAG(1000,2, 10, 5, 10, 0.7F)
                .toBuilder()
                .correlationID(correlationID)
                .build();

        request.getVertices().values().forEach(task -> task.controllerMode = Mode.IDLE);

        taskEndpoint.start(request);

        Set<TaskDTO> tasks = taskEndpoint.byCorrelation(correlationID);

        assertThat(tasks)
                .isNotEmpty()
                .extracting(task -> task.correlationID)
                .containsOnly(correlationID);
    }

    @Test
    void testCorrelationIsNullWhenNotSpecified() {
        CreateGraphRequest request = generateDAG(1000,2, 10, 5, 10, 0.7F);

        taskEndpoint.start(request);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters());

        assertThat(all)
                .isNotEmpty()
                .extracting(task -> task.correlationID)
                .containsOnlyNulls();
    }

    @Test
    void testQueryByNonExistingCorrelationID() {
        String correlationID = "trash-metal";
        CreateGraphRequest request = generateDAG(1000,2, 10, 5, 10, 0.7F)
                .toBuilder()
                .correlationID(correlationID)
                .build();

        taskEndpoint.start(request);

        Set<TaskDTO> tasks = taskEndpoint.byCorrelation("non-existing-id");

        assertThat(tasks).isEmpty();
    }
}
