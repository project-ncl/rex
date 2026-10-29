/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.test.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.test.common.TestData.getAllParameters;

import java.util.Set;

import jakarta.inject.Inject;

import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CorrelationTest extends AbstractTest {

    @Inject
    TaskEndpoint taskEndpoint;

    @Test
    void testAllTasksGetCorrelated() {
        String correlationID = "heavy-metal";

        CreateGraphRequest request = generateDAG(1000, 2, 10, 5, 10, 0.7F)
                .toBuilder()
                .correlationID(correlationID)
                .build();

        Set<TaskDTO> start = taskEndpoint.start(request);

        assertThat(start).extracting(task -> task.correlationID).containsOnly(correlationID);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters(), null);

        assertThat(all)
                .isNotEmpty()
                .extracting(task -> task.correlationID)
                .containsOnly(correlationID);
    }

    @Test
    void testCorrelationGetAllEndpoint() throws InterruptedException {
        String correlationID = "nu-metal";

        CreateGraphRequest request = generateDAG(1000, 2, 10, 5, 10, 0.7F)
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
        CreateGraphRequest request = generateDAG(1000, 2, 10, 5, 10, 0.7F);

        taskEndpoint.start(request);

        Set<TaskDTO> all = taskEndpoint.getAll(getAllParameters(), null);

        assertThat(all)
                .isNotEmpty()
                .extracting(task -> task.correlationID)
                .containsOnlyNulls();
    }

    @Test
    void testQueryByNonExistingCorrelationID() {
        String correlationID = "trash-metal";
        CreateGraphRequest request = generateDAG(1000, 2, 10, 5, 10, 0.7F)
                .toBuilder()
                .correlationID(correlationID)
                .build();

        taskEndpoint.start(request);

        Set<TaskDTO> tasks = taskEndpoint.byCorrelation("non-existing-id");

        assertThat(tasks).isEmpty();
    }
}
