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

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.rex.test.common.Assertions.assertCorrectTaskRelations;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAre;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.test.common.TestData.getComplexGraph;
import static org.jboss.pnc.rex.test.common.TestData.getEndpointWithStart;
import static org.jboss.pnc.rex.test.common.TestData.getMockTaskWithStart;
import static org.jboss.pnc.rex.test.common.RandomDAGGeneration.generateDAG;
import static org.jboss.pnc.rex.test.common.TestData.getMockTaskWithoutStart;
import static org.jboss.pnc.rex.test.common.TestData.getRequestWithStart;
import static org.jboss.pnc.rex.test.common.TestData.getRequestWithoutStart;
import static org.jboss.pnc.rex.test.common.TestData.getSingleWithoutStart;
import static org.jboss.pnc.rex.test.common.TestData.getStopRequest;
import static org.jboss.pnc.rex.test.common.TestData.getStopRequestWithCallback;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.TransactionManager;

import io.quarkus.test.security.TestSecurity;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.common.exceptions.CircularDependencyException;
import org.jboss.pnc.rex.common.exceptions.ConstraintConflictException;
import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.core.TaskContainerImpl;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.core.counter.Counter;
import org.jboss.pnc.rex.core.counter.Running;
import org.jboss.pnc.rex.test.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.Task;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@QuarkusTest
@Slf4j
@TestSecurity(authorizationEnabled = false)
class TaskContainerImplTest extends AbstractTest {

    public static final String EXISTING_KEY = "omg.wtf.whatt";

    @Inject
    TaskContainerImpl container;

    @Inject
    TaskController controller;

    @Inject
    TaskEndpoint taskEndpoint;

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskEndpointURI;

    @Inject
    HttpEndpoint httpEndpoint;

    @Inject
    @Running
    Counter running;

    @BeforeEach
    public void before() throws Exception {
        putDummyTask();
    }

    @Test
    public void testGet() {
        assertThat(container.getTask(EXISTING_KEY))
                .isNotNull();
        VersionedValue<Task> service = container.getCache().getWithMetadata(EXISTING_KEY);
        assertThat(service.getVersion()).isNotZero();
    }

    @Test
    public void testTransaction() throws Exception {
        TransactionManager tm = container.getCache().getTransactionManager();

        tm.begin();
        Task old = container.getTask(EXISTING_KEY);
        Request start = old.getRemoteStart().toBuilder().attachment("another useless string").build();
        container.getCache().put(old.getName(), old.toBuilder().remoteStart(start).build());
        running.replaceValue(0L, 10L);
        tm.setRollbackOnly();
        assertThatThrownBy(tm::commit)
                .isInstanceOf(RollbackException.class);

        assertThat(container.getTask(EXISTING_KEY).getRemoteStart().getAttachment()).isEqualTo("{id: 100}");
        assertThat(running.getValue()).isEqualTo(0);
    }

    @Test
    public void testInstall() throws Exception {
        taskEndpoint.start(CreateGraphRequest.builder()
                .edge(new EdgeDTO("service2", "service1"))
                .vertex("service1", CreateTaskDTO.builder()
                        .name("service1")
                        .controllerMode(Mode.IDLE)
                        .remoteStart(getRequestWithoutStart("I am service1!"))
                        .remoteCancel(getStopRequest("I am service1!"))
                        .build())
                .vertex("service2", CreateTaskDTO.builder()
                        .name("service2")
                        .controllerMode(Mode.IDLE)
                        .remoteStart(getRequestWithoutStart("I am service2!"))
                        .remoteCancel(getStopRequest("I am service2!"))
                        .build())
                .build());

        Task task1 = container.getTask("service1");
        assertThat(task1)
                .isNotNull();
        assertThat(task1.getDependants())
                .containsOnly("service2");

        Task task2 = container.getTask("service2");
        assertThat(task2)
                .isNotNull();
        assertThat(task2.getDependencies())
                .containsOnly("service1");
        assertThat(task2.getUnfinishedDependencies())
                .isEqualTo(1);
    }

    @Test
    public void testSingleServiceStarts() throws Exception {
        TransactionManager manager = container.getTransactionManager();
        manager.begin();
        controller.setMode(EXISTING_KEY, Mode.ACTIVE, true);
        manager.commit();

        waitTillTasksAre(State.UP, container, EXISTING_KEY);
        Task task = container.getTask(EXISTING_KEY);
        assertThat(task.getState()).isEqualTo(State.UP);
    }

    @Test
    public void testDependantWaiting() throws Exception {
        String dependant = "dependant.service";
        taskEndpoint.start(CreateGraphRequest.builder()
                .edge(new EdgeDTO(dependant, EXISTING_KEY))
                .vertex(dependant, CreateTaskDTO.builder()
                        .name(dependant)
                        .remoteStart(getRequestWithoutStart("A payload"))
                        .remoteCancel(getStopRequest("A payload"))
                        .controllerMode(Mode.ACTIVE)
                        .build())
                .build());

        Task task = container.getTask(dependant);
        assertThat(task)
                .isNotNull();
        assertThat(task.getState())
                .isEqualTo(State.WAITING);
    }

    @Test
    public void testDependantStartsThroughDependency() throws Exception {
        String dependant = "dependant.service";
        taskEndpoint.start(CreateGraphRequest.builder()
                .edge(new EdgeDTO(dependant, EXISTING_KEY))
                .vertex(dependant, CreateTaskDTO.builder()
                        .name(dependant)
                        .remoteStart(getRequestWithoutStart("A payload"))
                        .remoteCancel(getStopRequest("A payload"))
                        .controllerMode(Mode.ACTIVE)
                        .build())
                .build());

        container.getTransactionManager().begin();
        controller.setMode(EXISTING_KEY, Mode.ACTIVE, true);
        container.getTransactionManager().commit();

        waitTillTasksAre(State.UP, container, EXISTING_KEY);

        container.getTransactionManager().begin();
        controller.accept(EXISTING_KEY, null, Origin.REMOTE_ENTITY);
        container.getTransactionManager().commit();

        waitTillTasksAre(State.UP, container, dependant);
    }

    @Test
    public void testComplexInstallation() {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";
        String e = "e";
        String f = "f";
        String g = "g";
        String h = "h";
        String i = "i";
        String j = "j";

        taskEndpoint.start(getComplexGraph(false));

        assertCorrectTaskRelations(container.getTask(a), 0, new String[]{c, d}, null);
        assertCorrectTaskRelations(container.getTask(b), 0, new String[]{d, e, h}, null);
        assertCorrectTaskRelations(container.getTask(c), 1, new String[]{f}, new String[]{a});
        assertCorrectTaskRelations(container.getTask(d), 2, new String[]{e}, new String[]{a, b});
        assertCorrectTaskRelations(container.getTask(e), 2, new String[]{g, h}, new String[]{d, b});
        assertCorrectTaskRelations(container.getTask(f), 1, new String[]{i}, new String[]{c});
        assertCorrectTaskRelations(container.getTask(g), 1, new String[]{i, j}, new String[]{e});
        assertCorrectTaskRelations(container.getTask(h), 2, new String[]{j}, new String[]{e, b});
        assertCorrectTaskRelations(container.getTask(i), 2, null, new String[]{f, g});
        assertCorrectTaskRelations(container.getTask(j), 2, null, new String[]{g, h});
    }

    @Test
    public void testComplexInstallationWithAlreadyExistingService() throws Exception {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";
        String e = "e";
        String f = "f";
        String g = "g";
        String h = "h";
        String i = "i";
        String j = "j";

        CreateGraphRequest graph = getComplexGraph(false).toBuilder()
                .edge(new EdgeDTO(f, EXISTING_KEY))
                .edge(new EdgeDTO(EXISTING_KEY, c))
                .edge(new EdgeDTO(EXISTING_KEY, d))
                .build();

        taskEndpoint.start(graph);

        assertCorrectTaskRelations(container.getTask(a), 0, new String[]{c, d}, null);
        assertCorrectTaskRelations(container.getTask(b), 0, new String[]{d, e, h}, null);
        assertCorrectTaskRelations(container.getTask(c), 1, new String[]{f, EXISTING_KEY}, new String[]{a});
        assertCorrectTaskRelations(container.getTask(d), 2, new String[]{e, EXISTING_KEY}, new String[]{a, b});
        assertCorrectTaskRelations(container.getTask(e), 2, new String[]{g, h}, new String[]{d, b});
        assertCorrectTaskRelations(container.getTask(f), 2, new String[]{i}, new String[]{c, EXISTING_KEY});
        assertCorrectTaskRelations(container.getTask(g), 1, new String[]{i, j}, new String[]{e});
        assertCorrectTaskRelations(container.getTask(h), 2, new String[]{j}, new String[]{e, b});
        assertCorrectTaskRelations(container.getTask(i), 2, null, new String[]{f, g});
        assertCorrectTaskRelations(container.getTask(j), 2, null, new String[]{g, h});
        assertCorrectTaskRelations(container.getTask(EXISTING_KEY), 2, new String[]{f}, new String[]{c, d});
    }

    @Test
    public void testComplexWithCompletion() throws Exception {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";
        String e = "e";
        String f = "f";
        String g = "g";
        String h = "h";
        String i = "i";
        String j = "j";
        String[] services = new String[]{a, b, c, d, e, f, g, h, i, j, EXISTING_KEY};

        Task existingTask = container.getTask(EXISTING_KEY);
        Task updatedTask = existingTask.toBuilder().remoteStart(getEndpointWithStart(EXISTING_KEY)).build();
        container.getCache().put(EXISTING_KEY, updatedTask);

        CreateGraphRequest graph = getComplexGraph(true).toBuilder()
                .edge(new EdgeDTO(f, EXISTING_KEY))
                .edge(new EdgeDTO(EXISTING_KEY, c))
                .edge(new EdgeDTO(EXISTING_KEY, d))
                .build();

        taskEndpoint.start(graph);

        container.getCache().getTransactionManager().begin();
        controller.setMode(EXISTING_KEY, Mode.ACTIVE, true);
        container.getCache().getTransactionManager().commit();
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, services);

        // sleep because running counter takes time to update
        Thread.sleep(100);
        assertThat(running.getValue()).isEqualTo(0);
        assertThat(container.getTasks(true, true, true, true)).extracting("name", String.class)
                .doesNotContain(services);
    }

    @Test
    public void testCancellationWithDependencies() throws Exception {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";
        String e = "e";
        String f = "f";
        String g = "g";
        String h = "h";
        String i = "i";
        String j = "j";
        String k = "k";
        String[] services = new String[]{a, b, c, d, e, f, g, h, i, j, k};

        CreateGraphRequest request = CreateGraphRequest.builder()
                .edge(new EdgeDTO(b, a))
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(f, e))
                .edge(new EdgeDTO(f, b))
                .edge(new EdgeDTO(f, d))
                .edge(new EdgeDTO(g, f))
                .edge(new EdgeDTO(g, d))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, f))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, h))
                .edge(new EdgeDTO(j, f))
                .edge(new EdgeDTO(k, g))
                .edge(new EdgeDTO(k, j))
                .edge(new EdgeDTO(k, i))
                .vertex(a, getMockTaskWithStart(a, Mode.IDLE))
                .vertex(b, getMockTaskWithStart(b, Mode.IDLE))
                .vertex(c, getMockTaskWithStart(c, Mode.IDLE))
                .vertex(d, getMockTaskWithStart(d, Mode.IDLE))
                .vertex(e, getMockTaskWithStart(e, Mode.IDLE))
                .vertex(f, getMockTaskWithStart(f, Mode.IDLE))
                .vertex(g, getMockTaskWithStart(g, Mode.IDLE))
                .vertex(h, getMockTaskWithStart(h, Mode.IDLE))
                .vertex(i, getMockTaskWithStart(i, Mode.IDLE))
                .vertex(j, getMockTaskWithStart(j, Mode.IDLE))
                .vertex(k, getMockTaskWithStart(k, Mode.IDLE))
                .build();
        taskEndpoint.start(request);
        container.getCache().getTransactionManager().begin();
        controller.setMode(a, Mode.CANCEL);
        container.getCache().getTransactionManager().commit();

        waitTillTasksAreFinishedWith(State.STOPPED, services);

        assertThat(container.getTasks(true, true, true, true)).extracting("name", String.class)
                .doesNotContain(request.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    public void testQuery() throws Exception {
        taskEndpoint.start(getComplexGraph(false));

        assertThat(container.getTasks(true, true, true, true)).hasSize(11);
    }

    @Test
    public void testCycle() throws Exception {
        String a = "a";
        String i = "i";

        // a -> i creates a i->f->c->a->i cycle
        CreateGraphRequest request = getComplexGraph(false).toBuilder()
                .edge(new EdgeDTO(a, i))
                .build();

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(CircularDependencyException.class);
    }

    @Test
    public void testValidConfigWithNoNotificationAndWaiting() {
        CreateGraphRequest request = CreateGraphRequest.builder()
                .vertex("ignore", getMockTaskWithoutStart("ignore", Mode.IDLE)
                        .toBuilder()
                        .callerNotifications(null)
                        .configuration(ConfigurationDTO.builder().delayDependantsForFinalNotification(true).build())
                        .build())
                .build();

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldFailOnAddingDependencyToNonIdleTask() throws Exception {
        TransactionManager manager = container.getTransactionManager();
        manager.begin();
        controller.setMode(EXISTING_KEY, Mode.ACTIVE, true);
        manager.commit();

        CreateGraphRequest request = getSingleWithoutStart("newDependency").toBuilder()
                .edge(new EdgeDTO(EXISTING_KEY,"newDependency"))
                .build();

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldFailOnTryingToScheduleExistingTask() {
        CreateGraphRequest request = getSingleWithoutStart(EXISTING_KEY);

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(TaskConflictException.class);
    }

    @Test
    public void shouldFailOnTryingToCreateReflexiveEdge() {

        CreateGraphRequest request = CreateGraphRequest.builder()
                .edge(new EdgeDTO(EXISTING_KEY, EXISTING_KEY))
                .build();

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldFailOnHavingATaskWithoutData() {
        CreateGraphRequest request = CreateGraphRequest.builder()
                .edge(new EdgeDTO(EXISTING_KEY, "task with no data"))
                .vertex("ignore", getMockTaskWithoutStart("ignore", Mode.IDLE))
                .build();

        assertThatThrownBy(() -> taskEndpoint.start(request))
                .isInstanceOf(BadRequestException.class);
    }


    @Test
    void shouldFailOnStartingTasksWithSameConstraint() {
        CreateTaskDTO withConstraint1 = getMockTaskWithoutStart("with-constraint1", Mode.IDLE).toBuilder()
                .constraint("common")
                .build();
        CreateTaskDTO withConstraint2 = getMockTaskWithoutStart("with-constraint2", Mode.IDLE).toBuilder()
                .constraint("common")
                .build();

        CreateGraphRequest firstRequest =  CreateGraphRequest.builder()
                .vertex("with-constraint1", withConstraint1)
                .build();
        CreateGraphRequest secondRequest =  CreateGraphRequest.builder()
                .vertex("with-constraint2", withConstraint2)
                .build();

        taskEndpoint.start(firstRequest);

        assertThatThrownBy(() -> taskEndpoint.start(secondRequest))
                .isInstanceOf(ConstraintConflictException.class);

    }

    @Test
    void testConstraintIsDeletedAfterTaskCompletes() {
        String taskUno = "with-constraint1";
        String taskDos = "with-constraint2";

        CreateTaskDTO withConstraint1 = getMockTaskWithStart(taskUno, Mode.ACTIVE).toBuilder()
                .constraint("common")
                .build();

        CreateGraphRequest firstRequest =  CreateGraphRequest.builder()
                .vertex(taskUno, withConstraint1)
                .build();
        taskEndpoint.start(firstRequest);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskUno);


        // DO SECOND REQUEST WITH THE SAME CONSTRAINT
        CreateTaskDTO withConstraint2 = getMockTaskWithStart(taskDos, Mode.ACTIVE).toBuilder()
                .constraint("common")
                .build();
        CreateGraphRequest secondRequest =  CreateGraphRequest.builder()
                .vertex(taskDos, withConstraint2)
                .build();
        taskEndpoint.start(secondRequest);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, taskDos);
    }

    /**
     * Generates random DAG graph, and tests whether all Task have finished
     * <p>
     * Great way to find running conditions
     * <p>
     * Disabled for test determinism reasons
     */
    @RepeatedTest(100)
    @Disabled
    public void randomDAGTest() throws Exception {
        CreateGraphRequest randomDAG = generateDAG(2, 10, 5, 10, 0.7F);
        taskEndpoint.start(randomDAG);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, randomDAG.getVertices().keySet().toArray(new String[0]));

        // sleep because running counter takes time to update
        Thread.sleep(50);
        assertThat(running.getValue()).isEqualTo(0);
        assertThat(container.getTasks(true, true, true, true)).extracting("name", String.class)
                .doesNotContain(randomDAG.getVertices().keySet().toArray(new String[0]));
    }

    @Test
    public void testGettingTaskResults() throws Exception {

        httpEndpoint.startRecordingQueue();

        taskEndpoint.start(CreateGraphRequest.builder()
                .edge(new EdgeDTO("service2", "service1"))
                .vertex("service1", CreateTaskDTO.builder()
                        .name("service1")
                        .remoteStart(getRequestWithStart("I am service1!"))
                        .remoteCancel(getStopRequestWithCallback("I am service1!"))
                        .build())
                .vertex("service2", CreateTaskDTO.builder()
                        .name("service2")
                        .remoteStart(getRequestWithStart("I am service2!"))
                        .remoteCancel(getStopRequestWithCallback("I am service2!"))
                        .configuration(new ConfigurationDTO(true, false, false, null, null, false))
                        .build())
                .build());

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "service1", "service2");

        httpEndpoint.stopRecording();
        Collection<Object> requests = httpEndpoint.getRecordedRequestData();
        assertThat(requests).hasSize(2);

        // the last request should have the taskResults populated
        Object lastRequest = requests.toArray()[requests.size() - 1];
        StartRequest startRequest = (StartRequest) lastRequest;

        Map<String, Object> taskResults = startRequest.getTaskResults();
        assertThat(taskResults).isNotEmpty();
        assertThat(taskResults.keySet()).contains("service1");
        assertThat(taskResults.get("service1")).isNotNull();
    }

    @Test
    public void testNotGettingTaskResultsWhenConfigurationProhibitsIt() throws Exception {

        httpEndpoint.startRecordingQueue();

        taskEndpoint.start(CreateGraphRequest.builder()
                .edge(new EdgeDTO("service2", "service1"))
                .vertex("service1", CreateTaskDTO.builder()
                        .name("service1")
                        .remoteStart(getRequestWithStart("I am service1!"))
                        .remoteCancel(getStopRequestWithCallback("I am service1!"))
                        .build())
                .vertex("service2", CreateTaskDTO.builder()
                        .name("service2")
                        .remoteStart(getRequestWithStart("I am service2!"))
                        .remoteCancel(getStopRequestWithCallback("I am service2!"))
                        .configuration(new ConfigurationDTO(false, false, false, null, null, false))
                        .build())
                .build());

        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "service1", "service2");

        httpEndpoint.stopRecording();
        Collection<Object> requests = httpEndpoint.getRecordedRequestData();

        // the last request should have the taskResults populated
        Object lastRequest = requests.toArray()[requests.size() - 1];
        StartRequest startRequest = (StartRequest) lastRequest;

        Map<String, Object> taskResults = startRequest.getTaskResults();
        assertThat(taskResults).isNull();
    }

    private void putDummyTask() {
        CreateGraphRequest dummyTask = CreateGraphRequest.builder()
            .vertex(EXISTING_KEY, CreateTaskDTO.builder()
                .name(EXISTING_KEY)
                .controllerMode(Mode.IDLE)
                .remoteStart(getRequestWithoutStart("{id: 100}"))
                .remoteCancel(getStopRequest("{id: 100}"))
                .build())
            .build();

        Set<TaskDTO> response = with()
            .body(dummyTask)
                .contentType(ContentType.JSON)
            .post(taskEndpointURI.getPath())
            .then()
                .statusCode(200)
            .extract().as(new TypeRef<>(){});

        assertThat(response)
            .isNotNull()
            .extracting("name")
            .contains(EXISTING_KEY);
    }
}