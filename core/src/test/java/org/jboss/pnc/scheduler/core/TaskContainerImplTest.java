package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.infinispan.client.hotrod.MetadataValue;
import java.lang.String;
import org.jboss.pnc.scheduler.common.exceptions.CircularDependencyException;
import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskBuilder;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.rest.api.InternalEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
//@QuarkusTestResource(InfinispanResource.class)
class TaskContainerImplTest {

    private static final Logger log = LoggerFactory.getLogger(TaskContainerImplTest.class);

    @Inject
    TaskContainerImpl container;

    @Inject
    ManagedExecutor executor;

    @Inject
    InternalEndpoint endpoint;

    @BeforeEach
    public void before() throws Exception {
        container.getCache().clear();
        BatchTaskInstaller installer = container.addTasks();
        installer
                .addTask("omg.wtf.whatt")
                .setPayload("{id: 100}")
                .setRemoteEndpoints(getMockAPI())
                .setInitialMode(Mode.IDLE)
                .install();
        installer.commit();

    }

    @Test
    public void testGet() {
        assertThat(container.getTask("omg.wtf.whatt"))
                .isNotNull();
        MetadataValue<Task> service = container.getCache().getWithMetadata("omg.wtf.whatt");
        assertThat(service.getVersion()).isNotZero();
    }

    @Test
    public void testTransaction() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
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
        BatchTaskInstaller installer = container.addTasks();
        installer.addTask("service1")
                .setInitialMode(Mode.IDLE)
                .isRequiredBy("service2")
                .setPayload("SAOIDHSAOIDHSALKDH LKSA")
                .install();

        installer.addTask("service2")
                .setInitialMode(Mode.IDLE)
                .requires("service1")
                .setPayload("ASDLJSALKJDHSAKJDHLKJSAHDLKJSAHDK")
                .install();


        installer.commit();
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
        TaskController controller = container.getTaskController("omg.wtf.whatt");
        TransactionManager manager = container.getTransactionManager();
        manager.begin();
        controller.setMode(Mode.ACTIVE);
        manager.commit();

        waitTillServicesAre(State.UP, container.getTask("omg.wtf.whatt"));
        Task task = container.getTask("omg.wtf.whatt");
        assertThat(task.getState()).isEqualTo(State.UP);

    }

    @Test
    public void testDependantWaiting() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        String dependant = "dependant.service";
        batchTaskInstaller.addTask(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires("omg.wtf.whatt")
                .setInitialMode(Mode.ACTIVE)
                .setPayload("A Payload")
                .install();
        batchTaskInstaller.commit();
        Task task = container.getTask(dependant);
        assertThat(task)
                .isNotNull();
        assertThat(task.getState())
                .isEqualTo(State.WAITING);
    }

    @Test
    public void testDependantStartsThroughDependency() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        String dependant = "dependant.service";
        batchTaskInstaller.addTask(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires("omg.wtf.whatt")
                .setInitialMode(Mode.ACTIVE)
                .setPayload("A Payload")
                .install();
        batchTaskInstaller.commit();

        TaskController dependencyController = container.getTaskController("omg.wtf.whatt");
        container.getTransactionManager().begin();
        dependencyController.setMode(Mode.ACTIVE);
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, "omg.wtf.whatt");

        container.getTransactionManager().begin();
        dependencyController.accept();
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, dependant);
    }

    @Test
    public void testComplexInstallation() {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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

        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f}, new String[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{e}, new String[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{g, h}, new String[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{i}, new String[]{c});
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, j}, new String[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new String[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new String[]{g, h});
        batchTaskInstaller.commit();

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
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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

        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f, existing}, new String[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{e, existing}, new String[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{g, h}, new String[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{i}, new String[]{c, existing});
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, j}, new String[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new String[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new String[]{g, h});
        batchTaskInstaller.commit();

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
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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

        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{c, d}, null, getMockWithStart(), a);
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{d, e, h}, null, getMockWithStart(), b);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f, existing}, new String[]{a}, getMockWithStart(), c);
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{e, existing}, new String[]{a, b}, getMockWithStart(), d);
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{g, h}, new String[]{d, b}, getMockWithStart(), e);
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{i}, new String[]{c, existing}, getMockWithStart(), f);
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, j}, new String[]{e}, getMockWithStart(), g);
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, b}, getMockWithStart(), h);
        installService(batchTaskInstaller, i, Mode.IDLE, null, new String[]{f, g}, getMockWithStart(), i);
        installService(batchTaskInstaller, j, Mode.IDLE, null, new String[]{g, h}, getMockWithStart(), j);
        batchTaskInstaller.commit();

        container.getTransactionManager().begin();
        for (String name : services) {
            container.getTaskController(name).setMode(Mode.ACTIVE);
        }
        container.getTransactionManager().commit();

        waitTillServicesAre(State.SUCCESSFUL, services);
    }

    @Test
    public void testCancellationWithDependencies() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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

        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{b, c, d}, null, getMockWithStart(), a);
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{e, f}, new String[]{a}, getMockWithStart(), b);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f}, new String[]{a}, getMockWithStart(), c);
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{f, g}, new String[]{a}, getMockWithStart(), d);
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{h, f}, new String[]{b}, getMockWithStart(), e);
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{g, h, i, j}, new String[]{e, b, c, d}, getMockWithStart(), f);
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, k}, new String[]{f, d}, getMockWithStart(), g);
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, f}, getMockWithStart(), h);
        installService(batchTaskInstaller, i, Mode.IDLE, new String[]{k}, new String[]{f, g}, getMockWithStart(), i);
        installService(batchTaskInstaller, j, Mode.IDLE, new String[]{k}, new String[]{h, f}, getMockWithStart(), j);
        installService(batchTaskInstaller, k, Mode.IDLE, null, new String[]{g, j, i}, getMockWithStart(), j);
        batchTaskInstaller.commit();
        container.getCache().getTransactionManager().begin();
        container.getTaskController(a).setMode(Mode.CANCEL);
        container.getCache().getTransactionManager().commit();

        waitTillServicesAre(State.STOPPED, services);

    }

    @Test
    public void testTransactionPropagation() throws Exception {
        TransactionManager tm = container.getTransactionManager();
        tm.begin();
        executor.submit(
                () -> {
                    try {
                        Transaction transaction = tm.getTransaction();
                        assertThat(transaction).isNotNull();
                        assertThat(transaction.getStatus())
                                .isEqualTo(Status.STATUS_ACTIVE);
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).get();
        tm.commit();
    }

    @Test
    public void testQuery() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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

        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f}, new String[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{e}, new String[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{g, h}, new String[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{i}, new String[]{c});
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, j}, new String[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new String[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new String[]{g, h});
        batchTaskInstaller.commit();

        assertThat(container.getTask(true, true, true)).hasSize(11);
    }

    @Test
    public void testCycle() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
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
        installService(batchTaskInstaller, a, Mode.IDLE, new String[]{c, d}, new String[]{i});
        installService(batchTaskInstaller, b, Mode.IDLE, new String[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new String[]{f}, new String[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new String[]{e}, new String[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new String[]{g, h}, new String[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new String[]{i}, new String[]{c});
        installService(batchTaskInstaller, g, Mode.IDLE, new String[]{i, j}, new String[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new String[]{j}, new String[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, new String[]{a}, new String[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new String[]{g, h});

        assertThatThrownBy(batchTaskInstaller::commit)
                .isInstanceOf(CircularDependencyException.class);
    }

    private static void installService(BatchTaskInstaller batchTaskInstaller, String name, Mode mode, String[] dependants, String[] dependencies) {
        installService(batchTaskInstaller, name, mode, dependants, dependencies, getMockAPI(), String.format("I'm an %s!", name));
    }

    private static void installService(BatchTaskInstaller batchTaskInstaller, String name, Mode mode, String[] dependants, String[] dependencies, RemoteAPI remoteAPI, String payload) {
        TaskBuilder builder = batchTaskInstaller.addTask(name)
                .setPayload(payload)
                .setInitialMode(mode)
                .setRemoteEndpoints(remoteAPI);
        if (dependants != null)
            for (String dependant : dependants) {
                builder.isRequiredBy(dependant);
            }
        if (dependencies != null)
            for (String dependency : dependencies) {
                builder.requires(dependency);
            }
        builder.install();
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

    private void waitTillServicesAre(State state, String... Strings) {
        List<String> fine = new ArrayList<>(Arrays.asList(Strings));
        waitSynchronouslyFor(() -> {
            Iterator<String> iterator = fine.iterator();
            while (iterator.hasNext()) {
                Task s = container.getTask(iterator.next());
                if (s.getState().equals(state))
                    iterator.remove();
            }
            return fine.isEmpty();
        }, 10, TimeUnit.SECONDS);
    }

    public static void waitSynchronouslyFor(Supplier<Boolean> condition, long timeout, TimeUnit timeUnit) {
        long stopTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interruption", e);
            }
            if (System.currentTimeMillis() > stopTime) {
                throw new AssertionError("Timeout " + timeout + " " + timeUnit + " reached while waiting for condition");
            }
        } while (!condition.get());
    }

    private static RemoteAPI getMockAPI() {
        return RemoteAPI.builder()
                .startUrl("http://localhost:8081/test/accept")
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