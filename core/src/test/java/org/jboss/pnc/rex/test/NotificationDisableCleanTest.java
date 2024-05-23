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
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.test.profile.WithoutTaskCleaning;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.TestData.getComplexGraph;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@TestProfile(WithoutTaskCleaning.class) // disable deletion of tasks
public class NotificationDisableCleanTest extends AbstractTest {

    @Inject
    TaskEndpoint endpoint;

    @Test
    void testRecordedBodies() throws InterruptedException {
        CreateGraphRequest request = getComplexGraph(true, true);
        endpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, request.getVertices().keySet().toArray(new String[0]));

        Thread.sleep(100);
        Set<TaskDTO> all = endpoint.getAll(TestData.getAllParameters());
        Predicate<TaskDTO> sizePredicate = (task) -> task.getServerResponses() != null
                && task.getServerResponses().size() == 2;
        Predicate<TaskDTO> responsePredicate = (task) -> {
            var responses = task.getServerResponses();
            boolean firstBody = responses.stream().anyMatch((response ->
                    response.getState() == State.STARTING
                    && response.getBody() instanceof Map
                    && !((Map<String, String>) response.getBody()).get("task").isEmpty()));
            boolean secondBody = responses.stream().anyMatch((response ->
                    response.getState() == State.UP
                    && response.getBody() instanceof String
                    && response.getBody().equals("ALL IS OK")));
            return firstBody && secondBody;
        };
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(sizePredicate);
        assertThat(all).allMatch(responsePredicate);
    }
}
