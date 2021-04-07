package org.jboss.pnc.scheduler.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.scheduler.core.generation.RandomDAGGeneration.generateDAG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.exceptions.CircularDependencyException;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.jboss.pnc.scheduler.dto.EdgeDTO;
import org.jboss.pnc.scheduler.dto.RemoteLinksDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.rest.api.TaskEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TaskContainerImplTest {

    private static final Logger log = LoggerFactory.getLogger(TaskContainerImplTest.class);

    @Inject
    TaskContainerImpl container;

    @Inject
    TaskController controller;

    @Inject
    TaskEndpoint taskEndpoint;

    @BeforeEach
    public void before() throws Exception {
        container.getCache().clear();
        taskEndpoint.create(CreateGraphRequest.builder()
                .vertex("omg.wtf.whatt", CreateTaskDTO.builder()
                        .name("omg.wtf.whatt")
                        .controllerMode(Mode.IDLE)
                        .remoteLinks(getMockDTOAPI())
                        .payload("{id: 100}")
                        .build())
                .build());
    }

    @AfterEach
    public void after() throws Exception {
        container.getCache().clear();
    }

    @Test
    public void testGet() {
        assertThat(container.getTask("omg.wtf.whatt"))
                .isNotNull();
        MetadataValue<Task> service = container.getCache().getWithMetadata("omg.wtf.whatt");
        assertThat(service.getVersion()).isNotZero();
    }

    @Test
    public void testTransaction() throws Exception {
        TransactionManager tm = container.getCache().getTransactionManager();

        tm.begin();
        Task old = container.getTask("omg.wtf.whatt").toBuilder().payload("another useless string").build();
        container.getCache().put(old.getName(), old);
        tm.setRollbackOnly();
        assertThatThrownBy(tm::commit)
                .isInstanceOf(RollbackException.class);

        assertThat(container.getTask("omg.wtf.whatt").getPayload()).isEqualTo("{id: 100}");
    }

    @Test
    public void testInstall() throws Exception {
        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO("service2", "service1"))
                .vertex("service1", CreateTaskDTO.builder()
                        .name("service1")
                        .controllerMode(Mode.IDLE)
                        .payload("I am service1!")
                        .build())
                .vertex("service2", CreateTaskDTO.builder()
                        .name("service2")
                        .controllerMode(Mode.IDLE)
                        .payload("I am service2!")
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
        controller.setMode("omg.wtf.whatt", Mode.ACTIVE);
        manager.commit();

        waitTillServicesAre(State.UP, container.getTask("omg.wtf.whatt"));
        Task task = container.getTask("omg.wtf.whatt");
        assertThat(task.getState()).isEqualTo(State.UP);
    }

    @Test
    public void testDependantWaiting() throws Exception {
        String dependant = "dependant.service";
        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(dependant, "omg.wtf.whatt"))
                .vertex(dependant, CreateTaskDTO.builder()
                        .name(dependant)
                        .payload("A payload")
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
        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(dependant, "omg.wtf.whatt"))
                .vertex(dependant, CreateTaskDTO.builder()
                        .name(dependant)
                        .payload("A payload")
                        .controllerMode(Mode.ACTIVE)
                        .remoteLinks(getMockDTOAPI())
                        .build())
                .build());

        container.getTransactionManager().begin();
        controller.setMode("omg.wtf.whatt", Mode.ACTIVE);
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, "omg.wtf.whatt");

        container.getTransactionManager().begin();
        controller.accept("omg.wtf.whatt");
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, dependant);
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
        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
                .vertex(a, getMockTaskWithoutStart(a, Mode.IDLE))
                .vertex(b, getMockTaskWithoutStart(b, Mode.IDLE))
                .vertex(c, getMockTaskWithoutStart(c, Mode.IDLE))
                .vertex(d, getMockTaskWithoutStart(d, Mode.IDLE))
                .vertex(e, getMockTaskWithoutStart(e, Mode.IDLE))
                .vertex(f, getMockTaskWithoutStart(f, Mode.IDLE))
                .vertex(g, getMockTaskWithoutStart(g, Mode.IDLE))
                .vertex(h, getMockTaskWithoutStart(h, Mode.IDLE))
                .vertex(i, getMockTaskWithoutStart(i, Mode.IDLE))
                .vertex(j, getMockTaskWithoutStart(j, Mode.IDLE))
                .build());

        assertCorrectServiceRelations(container.getTask(a), 0, new String[]{c, d}, null);
        assertCorrectServiceRelations(container.getTask(b), 0, new String[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getTask(c), 1, new String[]{f}, new String[]{a});
        assertCorrectServiceRelations(container.getTask(d), 2, new String[]{e}, new String[]{a, b});
        assertCorrectServiceRelations(container.getTask(e), 2, new String[]{g, h}, new String[]{d, b});
        assertCorrectServiceRelations(container.getTask(f), 1, new String[]{i}, new String[]{c});
        assertCorrectServiceRelations(container.getTask(g), 1, new String[]{i, j}, new String[]{e});
        assertCorrectServiceRelations(container.getTask(h), 2, new String[]{j}, new String[]{e, b});
        assertCorrectServiceRelations(container.getTask(i), 2, null, new String[]{f, g});
        assertCorrectServiceRelations(container.getTask(j), 2, null, new String[]{g, h});
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
        String existing = "omg.wtf.whatt";

        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(f, existing))
                .edge(new EdgeDTO(existing, c))
                .edge(new EdgeDTO(existing, d))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
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
                .build());

        assertCorrectServiceRelations(container.getTask(a), 0, new String[]{c, d}, null);
        assertCorrectServiceRelations(container.getTask(b), 0, new String[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getTask(c), 1, new String[]{f, existing}, new String[]{a});
        assertCorrectServiceRelations(container.getTask(d), 2, new String[]{e, existing}, new String[]{a, b});
        assertCorrectServiceRelations(container.getTask(e), 2, new String[]{g, h}, new String[]{d, b});
        assertCorrectServiceRelations(container.getTask(f), 2, new String[]{i}, new String[]{c, existing});
        assertCorrectServiceRelations(container.getTask(g), 1, new String[]{i, j}, new String[]{e});
        assertCorrectServiceRelations(container.getTask(h), 2, new String[]{j}, new String[]{e, b});
        assertCorrectServiceRelations(container.getTask(i), 2, null, new String[]{f, g});
        assertCorrectServiceRelations(container.getTask(j), 2, null, new String[]{g, h});
        assertCorrectServiceRelations(container.getTask(existing), 2, new String[]{f}, new String[]{c, d});
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
        String existing = "omg.wtf.whatt";
        String[] services = new String[]{a, b, c, d, e, f, g, h, i, existing};

        Task existingTask = container.getTask(existing);
        Task updatedTask = existingTask.toBuilder().remoteEndpoints(getMockWithStart()).payload(existing).build();
        container.getCache().put(existing, updatedTask);

        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(f, existing))
                .edge(new EdgeDTO(existing, c))
                .edge(new EdgeDTO(existing, d))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
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
                .build());

        container.getTransactionManager().begin();
        for (String name : services) {
            controller.setMode(name, Mode.ACTIVE);
        }
        container.getTransactionManager().commit();

        waitTillServicesAre(State.SUCCESSFUL, services);
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

        taskEndpoint.create(CreateGraphRequest.builder()
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
                .build());
        container.getCache().getTransactionManager().begin();
        controller.setMode(a, Mode.CANCEL);
        container.getCache().getTransactionManager().commit();

        waitTillServicesAre(State.STOPPED, services);
    }

    @Test
    public void testQuery() throws Exception {
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

        taskEndpoint.create(CreateGraphRequest.builder()
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
                .vertex(a, getMockTaskWithoutStart(a, Mode.IDLE))
                .vertex(b, getMockTaskWithoutStart(b, Mode.IDLE))
                .vertex(c, getMockTaskWithoutStart(c, Mode.IDLE))
                .vertex(d, getMockTaskWithoutStart(d, Mode.IDLE))
                .vertex(e, getMockTaskWithoutStart(e, Mode.IDLE))
                .vertex(f, getMockTaskWithoutStart(f, Mode.IDLE))
                .vertex(g, getMockTaskWithoutStart(g, Mode.IDLE))
                .vertex(h, getMockTaskWithoutStart(h, Mode.IDLE))
                .vertex(i, getMockTaskWithoutStart(i, Mode.IDLE))
                .vertex(j, getMockTaskWithoutStart(j, Mode.IDLE))
                .build());

        assertThat(container.getTask(true, true, true)).hasSize(11);
    }

    @Test
    public void testCycle() throws Exception {
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

        // a -> i creates a i->f->c->a->i cycle
        CreateGraphRequest request = CreateGraphRequest.builder()
                .edge(new EdgeDTO(a, i))
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
                .vertex(a, getMockTaskWithoutStart(a, Mode.IDLE))
                .vertex(b, getMockTaskWithoutStart(b, Mode.IDLE))
                .vertex(c, getMockTaskWithoutStart(c, Mode.IDLE))
                .vertex(d, getMockTaskWithoutStart(d, Mode.IDLE))
                .vertex(e, getMockTaskWithoutStart(e, Mode.IDLE))
                .vertex(f, getMockTaskWithoutStart(f, Mode.IDLE))
                .vertex(g, getMockTaskWithoutStart(g, Mode.IDLE))
                .vertex(h, getMockTaskWithoutStart(h, Mode.IDLE))
                .vertex(i, getMockTaskWithoutStart(i, Mode.IDLE))
                .vertex(j, getMockTaskWithoutStart(j, Mode.IDLE))
                .build();

        assertThatThrownBy(() -> taskEndpoint.create(request))
                .isInstanceOf(CircularDependencyException.class);
    }

    /**
     * Generates random DAG graph, and tests whether all Task have finished
     *
     * Great way to find running conditions
     *
     * Disabled for test determinism reasons
     */
    @RepeatedTest(100)
    @Disabled
    public void randomDAGTest() throws Exception {
        CreateGraphRequest randomDAG = generateDAG(2, 10, 5, 10, 0.7F);
        taskEndpoint.create(randomDAG);
        waitTillServicesAre(State.SUCCESSFUL, randomDAG.getVertices().keySet().toArray(new String[0]));
    }

    private CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode) {
        return getMockTask(name, mode, getMockDTOAPI(), String.format("I'm an %s!", name));
    }
    private CreateTaskDTO getMockTaskWithStart(String name, Mode mode) {
        return getMockTask(name, mode, getStartingMockDTOAPI(), name);
    }

    private CreateTaskDTO getMockTask(String name, Mode mode, RemoteLinksDTO remoteLinks, String payload) {
        return CreateTaskDTO.builder()
                .name(name)
                .controllerMode(mode)
                .remoteLinks(remoteLinks)
                .payload(payload)
                .build();
    }

    private void assertCorrectServiceRelations(Task testing, int unfinishedDeps, String[] dependants, String[] dependencies) {
        assertThat(testing)
                .isNotNull();
        assertThat(testing.getUnfinishedDependencies())
                .isEqualTo(unfinishedDeps);
        if (dependants != null) {
            assertThat(testing.getDependants())
                    .containsExactlyInAnyOrder(dependants);
        }
        if (dependencies != null) {
            assertThat(testing.getDependencies())
                    .containsExactlyInAnyOrder(dependencies);
        }
    }

    private void waitTillServicesAre(State state, Task... tasks) {
        waitTillServicesAre(state, Arrays.stream(tasks).map(Task::getName).toArray(String[]::new));
    }

    private void waitTillServicesAre(State state, String... strings) {
        List<String> fine = new ArrayList<>(Arrays.asList(strings));
        waitSynchronouslyFor(() -> {
            Iterator<String> iterator = fine.iterator();
            while (iterator.hasNext()) {
                Task s = container.getTask(iterator.next());
                if (s.getState().equals(state))
                    iterator.remove();
            }
            return fine.isEmpty();
        }, 5, TimeUnit.SECONDS);
    }

    public static void waitSynchronouslyFor(Supplier<Boolean> condition, long timeout, TimeUnit timeUnit) {
        long stopTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interruption", e);
            }
            if (System.currentTimeMillis() > stopTime) {
                throw new AssertionError("Timeout " + timeout + " " + timeUnit + " reached while waiting for condition");
            }
        } while (!condition.get());
    }

    private static RemoteLinksDTO getMockDTOAPI() {
        return RemoteLinksDTO.builder()
                .startUrl("http://localhost:8081/test/accept")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }
    private static RemoteLinksDTO getStartingMockDTOAPI() {
        return RemoteLinksDTO.builder()
                .startUrl("http://localhost:8081/test/acceptAndStart")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }
    private static RemoteAPI getMockWithStart() {
        return RemoteAPI.builder()
                .startUrl("http://localhost:8081/test/acceptAndStart")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }
}