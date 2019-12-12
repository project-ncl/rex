package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchTaskInstaller;
import org.jboss.pnc.scheduler.core.api.TaskBuilder;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.model.Task;
import org.jboss.pnc.scheduler.common.enums.State;
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
import static org.jboss.msc.service.ServiceName.parse;

@QuarkusTest
class TaskContainerImplTest {

    private static final Logger log = LoggerFactory.getLogger(TaskContainerImplTest.class);

    @Inject
    TaskContainerImpl container;

    @Inject
    ManagedExecutor executor;

    @BeforeEach
    public void before() throws Exception {
        container.getCache().clear();
        BatchTaskInstaller installer = container.addTasks();
        installer
                .addTask(parse("omg.wtf.whatt"))
                .setPayload("{id: 100}")
                .setRemoteEndpoints(getMockAPI())
                .setInitialMode(Mode.IDLE)
                .install();
        installer.commit();

    }

    @Test
    public void testGet() {
        assertThat(container.getTask(parse("omg.wtf.whatt")))
                .isNotNull();
        MetadataValue<Task> service = container.getCache().getWithMetadata(parse("omg.wtf.whatt"));
        assertThat(service.getVersion()).isNotZero();
    }

    @Test
    public void testTransaction() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        TransactionManager tm = container.getCache().getTransactionManager();
        tm.begin();
        Task old = container.getTask(parse("omg.wtf.whatt")).toBuilder().payload("another useless string").build();
        container.getCache().put(old.getName(), old);
        tm.setRollbackOnly();
        assertThatThrownBy(tm::commit)
                .isInstanceOf(RollbackException.class);
        assertThat(container.getTask(parse("omg.wtf.whatt")).getPayload()).isEqualTo("{id: 100}");
    }

    @Test
    public void testInstall() throws Exception{
        BatchTaskInstaller installer = container.addTasks();
        installer.addTask(parse("service1"))
                .setInitialMode(Mode.IDLE)
                .isRequiredBy(parse("service2"))
                .setPayload("SAOIDHSAOIDHSALKDH LKSA")
                .install();

        installer.addTask(parse("service2"))
                .setInitialMode(Mode.IDLE)
                .requires(parse("service1"))
                .setPayload("ASDLJSALKJDHSAKJDHLKJSAHDLKJSAHDK")
                .install();


        installer.commit();
        Task task1 = container.getTask(parse("service1"));
        assertThat(task1)
                .isNotNull();
        assertThat(task1.getDependants())
                .containsOnly(parse("service2"));

        Task task2 = container.getTask(parse("service2"));
        assertThat(task2)
                .isNotNull();
        assertThat(task2.getDependencies())
                .containsOnly(parse("service1"));
        assertThat(task2.getUnfinishedDependencies())
                .isEqualTo(1);
    }

    @Test
    public void testSingleServiceStarts() throws Exception {
        TaskController controller = container.getTaskController(parse("omg.wtf.whatt"));
        TransactionManager manager = container.getTransactionManager();
        manager.begin();
        controller.setMode(Mode.ACTIVE);
        manager.commit();

        waitTillServicesAre(State.UP, container.getTask(parse("omg.wtf.whatt")));
        Task task = container.getTask(parse("omg.wtf.whatt"));
        assertThat(task.getState()).isEqualTo(State.UP);

    }

    @Test
    public void testDependantWaiting() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        ServiceName dependant = parse("dependant.service");
        batchTaskInstaller.addTask(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires(parse("omg.wtf.whatt"))
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
        ServiceName dependant = parse("dependant.service");
        batchTaskInstaller.addTask(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires(parse("omg.wtf.whatt"))
                .setInitialMode(Mode.ACTIVE)
                .setPayload("A Payload")
                .install();
        batchTaskInstaller.commit();

        TaskController dependencyController = container.getTaskController(parse("omg.wtf.whatt"));
        container.getTransactionManager().begin();
        dependencyController.setMode(Mode.ACTIVE);
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, parse("omg.wtf.whatt"));

        container.getTransactionManager().begin();
        dependencyController.accept();
        container.getTransactionManager().commit();

        waitTillServicesAre(State.UP, dependant);
    }

    @Test
    public void testComplexInstallation() {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        ServiceName a = parse("a");
        ServiceName b = parse("b");
        ServiceName c = parse("c");
        ServiceName d = parse("d");
        ServiceName e = parse("e");
        ServiceName f = parse("f");
        ServiceName g = parse("g");
        ServiceName h = parse("h");
        ServiceName i = parse("i");
        ServiceName j = parse("j");

        installService(batchTaskInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new ServiceName[]{f}, new ServiceName[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new ServiceName[]{e}, new ServiceName[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c});
        installService(batchTaskInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h});
        batchTaskInstaller.commit();

        assertCorrectServiceRelations(container.getTask(a), 0, new ServiceName[]{c, d}, null);
        assertCorrectServiceRelations(container.getTask(b), 0, new ServiceName[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getTask(c), 1, new ServiceName[]{f}, new ServiceName[]{a});
        assertCorrectServiceRelations(container.getTask(d), 2, new ServiceName[]{e}, new ServiceName[]{a, b});
        assertCorrectServiceRelations(container.getTask(e), 2, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        assertCorrectServiceRelations(container.getTask(f), 1, new ServiceName[]{i}, new ServiceName[]{c});
        assertCorrectServiceRelations(container.getTask(g), 1, new ServiceName[]{i, j}, new ServiceName[]{e});
        assertCorrectServiceRelations(container.getTask(h), 2, new ServiceName[]{j}, new ServiceName[]{e, b});
        assertCorrectServiceRelations(container.getTask(i), 2, null, new ServiceName[]{f, g});
        assertCorrectServiceRelations(container.getTask(j), 2, null, new ServiceName[]{g, h});
    }

    @Test
    public void testComplexInstallationWithAlreadyExistingService() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        ServiceName a = parse("a");
        ServiceName b = parse("b");
        ServiceName c = parse("c");
        ServiceName d = parse("d");
        ServiceName e = parse("e");
        ServiceName f = parse("f");
        ServiceName g = parse("g");
        ServiceName h = parse("h");
        ServiceName i = parse("i");
        ServiceName j = parse("j");
        ServiceName existing = parse("omg.wtf.whatt");

        installService(batchTaskInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new ServiceName[]{f, existing}, new ServiceName[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new ServiceName[]{e, existing}, new ServiceName[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c, existing});
        installService(batchTaskInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h});
        batchTaskInstaller.commit();

        assertCorrectServiceRelations(container.getTask(a), 0, new ServiceName[]{c, d}, null);
        assertCorrectServiceRelations(container.getTask(b), 0, new ServiceName[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getTask(c), 1, new ServiceName[]{f, existing}, new ServiceName[]{a});
        assertCorrectServiceRelations(container.getTask(d), 2, new ServiceName[]{e, existing}, new ServiceName[]{a, b});
        assertCorrectServiceRelations(container.getTask(e), 2, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        assertCorrectServiceRelations(container.getTask(f), 2, new ServiceName[]{i}, new ServiceName[]{c, existing});
        assertCorrectServiceRelations(container.getTask(g), 1, new ServiceName[]{i, j}, new ServiceName[]{e});
        assertCorrectServiceRelations(container.getTask(h), 2, new ServiceName[]{j}, new ServiceName[]{e, b});
        assertCorrectServiceRelations(container.getTask(i), 2, null, new ServiceName[]{f, g});
        assertCorrectServiceRelations(container.getTask(j), 2, null, new ServiceName[]{g, h});
        assertCorrectServiceRelations(container.getTask(existing), 2, new ServiceName[]{f}, new ServiceName[]{c, d});
    }

    @Test
    public void testComplexWithCompletion() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        ServiceName a = parse("a");
        ServiceName b = parse("b");
        ServiceName c = parse("c");
        ServiceName d = parse("d");
        ServiceName e = parse("e");
        ServiceName f = parse("f");
        ServiceName g = parse("g");
        ServiceName h = parse("h");
        ServiceName i = parse("i");
        ServiceName j = parse("j");
        ServiceName existing = parse("omg.wtf.whatt");
        ServiceName[] services = new ServiceName[]{a,b,c,d,e,f,g,h,i,existing};

        Task existingTask = container.getTask(existing);
        Task updatedTask = existingTask.toBuilder().remoteEndpoints(getMockWithStart()).payload(existing.getCanonicalName()).build();
        container.getCache().put(existing, updatedTask);

        installService(batchTaskInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null, getMockWithStart(), a.getCanonicalName());
        installService(batchTaskInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null, getMockWithStart(), b.getCanonicalName());
        installService(batchTaskInstaller, c, Mode.IDLE, new ServiceName[]{f, existing}, new ServiceName[]{a}, getMockWithStart(), c.getCanonicalName());
        installService(batchTaskInstaller, d, Mode.IDLE, new ServiceName[]{e, existing}, new ServiceName[]{a, b}, getMockWithStart(), d.getCanonicalName());
        installService(batchTaskInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b}, getMockWithStart(), e.getCanonicalName());
        installService(batchTaskInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c, existing}, getMockWithStart(), f.getCanonicalName());
        installService(batchTaskInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e}, getMockWithStart(), g.getCanonicalName());
        installService(batchTaskInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b}, getMockWithStart(), h.getCanonicalName());
        installService(batchTaskInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g}, getMockWithStart(), i.getCanonicalName());
        installService(batchTaskInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h}, getMockWithStart(), j.getCanonicalName());
        batchTaskInstaller.commit();

        container.getTransactionManager().begin();
        for (ServiceName name : services) {
            container.getTaskController(name).setMode(Mode.ACTIVE);
        }
        container.getTransactionManager().commit();

        waitTillServicesAre(State.SUCCESSFUL, services);
    }

    @Test
    public void testCancellationWithDependencies() throws Exception {
        BatchTaskInstaller batchTaskInstaller = container.addTasks();
        ServiceName a = parse("a");
        ServiceName b = parse("b");
        ServiceName c = parse("c");
        ServiceName d = parse("d");
        ServiceName e = parse("e");
        ServiceName f = parse("f");
        ServiceName g = parse("g");
        ServiceName h = parse("h");
        ServiceName i = parse("i");
        ServiceName j = parse("j");
        ServiceName k = parse("k");
        ServiceName[] services = new ServiceName[]{a,b,c,d,e,f,g,h,i,j,k};

        installService(batchTaskInstaller, a, Mode.IDLE, new ServiceName[]{b, c, d}, null, getMockWithStart(), a.getCanonicalName());
        installService(batchTaskInstaller, b, Mode.IDLE, new ServiceName[]{e, f}, new ServiceName[]{a}, getMockWithStart(), b.getCanonicalName());
        installService(batchTaskInstaller, c, Mode.IDLE, new ServiceName[]{f}, new ServiceName[]{a}, getMockWithStart(), c.getCanonicalName());
        installService(batchTaskInstaller, d, Mode.IDLE, new ServiceName[]{f, g}, new ServiceName[]{a}, getMockWithStart(), d.getCanonicalName());
        installService(batchTaskInstaller, e, Mode.IDLE, new ServiceName[]{h, f}, new ServiceName[]{b}, getMockWithStart(), e.getCanonicalName());
        installService(batchTaskInstaller, f, Mode.IDLE, new ServiceName[]{g, h, i, j}, new ServiceName[]{e, b, c, d}, getMockWithStart(), f.getCanonicalName());
        installService(batchTaskInstaller, g, Mode.IDLE, new ServiceName[]{i, k}, new ServiceName[]{f, d}, getMockWithStart(), g.getCanonicalName());
        installService(batchTaskInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, f}, getMockWithStart(), h.getCanonicalName());
        installService(batchTaskInstaller, i, Mode.IDLE, new ServiceName[]{k}, new ServiceName[]{f, g}, getMockWithStart(), i.getCanonicalName());
        installService(batchTaskInstaller, j, Mode.IDLE, new ServiceName[]{k}, new ServiceName[]{h, f}, getMockWithStart(), j.getCanonicalName());
        installService(batchTaskInstaller, k, Mode.IDLE, null, new ServiceName[]{g, j, i}, getMockWithStart(), j.getCanonicalName());
        batchTaskInstaller.commit();
        container.getCache().getTransactionManager().begin();
        container.getTaskController(a).setMode(Mode.CANCEL);
        container.getCache().getTransactionManager().commit();

        waitTillServicesAre(State.STOPPED, services);

    }

    @Test
    public void forFunPropagation() throws Exception {
        TransactionManager tm = container.getTransactionManager();
        tm.begin();
        executor.submit(
                () -> {
                    System.out.println(Thread.currentThread().getName());
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
        ServiceName a = parse("a");
        ServiceName b = parse("b");
        ServiceName c = parse("c");
        ServiceName d = parse("d");
        ServiceName e = parse("e");
        ServiceName f = parse("f");
        ServiceName g = parse("g");
        ServiceName h = parse("h");
        ServiceName i = parse("i");
        ServiceName j = parse("j");

        installService(batchTaskInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null);
        installService(batchTaskInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null);
        installService(batchTaskInstaller, c, Mode.IDLE, new ServiceName[]{f}, new ServiceName[]{a});
        installService(batchTaskInstaller, d, Mode.IDLE, new ServiceName[]{e}, new ServiceName[]{a, b});
        installService(batchTaskInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        installService(batchTaskInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c});
        installService(batchTaskInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e});
        installService(batchTaskInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b});
        installService(batchTaskInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g});
        installService(batchTaskInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h});
        batchTaskInstaller.commit();

        assertThat(container.getTask(true, true, true)).hasSize(11);
    }

    private static void installService(BatchTaskInstaller batchTaskInstaller, ServiceName name, Mode mode, ServiceName[] dependants, ServiceName[] dependencies) {
        installService(batchTaskInstaller,name, mode, dependants, dependencies, getMockAPI(), String.format("I'm an %s!", name));
    }

    private static void installService(BatchTaskInstaller batchTaskInstaller, ServiceName name, Mode mode, ServiceName[] dependants, ServiceName[] dependencies, RemoteAPI remoteAPI, String payload) {
        TaskBuilder builder = batchTaskInstaller.addTask(name)
                .setPayload(payload)
                .setInitialMode(mode)
                .setRemoteEndpoints(remoteAPI);
        if (dependants != null)
            for (ServiceName dependant : dependants) {
                builder.isRequiredBy(dependant);
            }
        if (dependencies != null)
            for (ServiceName dependency : dependencies) {
                builder.requires(dependency);
            }
        builder.install();
    }

    private void assertCorrectServiceRelations(Task testing, int unfinishedDeps, ServiceName[] dependants, ServiceName[] dependencies) {
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
        waitTillServicesAre(state, Arrays.stream(tasks).map(Task::getName).toArray(ServiceName[]::new));
    }

    private void waitTillServicesAre(State state, ServiceName... serviceNames) {
        List<ServiceName> fine = new ArrayList<>(Arrays.asList(serviceNames));
        waitSynchronouslyFor(() -> {
            Iterator<ServiceName> iterator = fine.iterator();
            while (iterator.hasNext()) {
                Task s = container.getTask(iterator.next());
                if (s.getState().equals(state))
                    iterator.remove();
            }
            return fine.isEmpty();
        }, 40, TimeUnit.SECONDS);
    }

    public static void waitSynchronouslyFor(Supplier<Boolean> condition, long timeout, TimeUnit timeUnit) {
        long stopTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interruption", e);
            }
            if(System.currentTimeMillis() > stopTime) {
                throw new AssertionError("Timeout " + timeout + " " + timeUnit + " reached while waiting for condition");
            }
        } while(!condition.get());
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