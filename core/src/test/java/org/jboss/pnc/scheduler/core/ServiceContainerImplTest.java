package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.BatchServiceInstaller;
import org.jboss.pnc.scheduler.core.api.ServiceBuilder;
import org.jboss.pnc.scheduler.core.api.ServiceController;
import org.jboss.pnc.scheduler.core.model.Mode;
import org.jboss.pnc.scheduler.core.model.RemoteAPI;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.core.model.State;
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
class ServiceContainerImplTest {

    private static final Logger log = LoggerFactory.getLogger(ServiceContainerImplTest.class);

    @Inject
    ServiceContainerImpl container;

    @BeforeEach
    public void before() throws Exception {
        container.getCache().clear();
        BatchServiceInstaller installer = container.addServices();
        installer
                .addService(parse("omg.wtf.whatt"))
                .setPayload("{id: 100}")
                .setRemoteEndpoints(getMockAPI())
                .setInitialMode(Mode.IDLE)
                .install();
        installer.commit();

    }

    @Test
    public void testGet() {
        assertThat(container.getService(parse("omg.wtf.whatt")))
                .isNotNull();
        MetadataValue<Service> service = container.getCache().getWithMetadata(parse("omg.wtf.whatt"));
        assertThat(service.getVersion()).isNotZero();
    }

    @Test
    public void testTransaction() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        TransactionManager tm = container.getCache().getTransactionManager();
        tm.begin();
        Service old = container.getService(parse("omg.wtf.whatt")).toBuilder().payload("another useless string").build();
        container.getCache().put(old.getName(), old);
        tm.setRollbackOnly();
        assertThatThrownBy(tm::commit)
                .isInstanceOf(RollbackException.class);
        assertThat(container.getService(parse("omg.wtf.whatt")).getPayload()).isEqualTo("{id: 100}");
    }

    @Test
    public void testInstall() throws Exception{
        BatchServiceInstaller installer = container.addServices();
        installer.addService(parse("service1"))
                .setInitialMode(Mode.IDLE)
                .isRequiredBy(parse("service2"))
                .setPayload("SAOIDHSAOIDHSALKDH LKSA")
                .install();

        installer.addService(parse("service2"))
                .setInitialMode(Mode.IDLE)
                .requires(parse("service1"))
                .setPayload("ASDLJSALKJDHSAKJDHLKJSAHDLKJSAHDK")
                .install();


        installer.commit();
        Service service1 = container.getService(parse("service1"));
        assertThat(service1)
                .isNotNull();
        assertThat(service1.getDependants())
                .containsOnly(parse("service2"));

        Service service2 = container.getService(parse("service2"));
        assertThat(service2)
                .isNotNull();
        assertThat(service2.getDependencies())
                .containsOnly(parse("service1"));
        assertThat(service2.getUnfinishedDependencies())
                .isEqualTo(1);
    }


//    @Test
//    public void testMockEndpoint() {
//        given().when().body("ASDKSALKDJ").post("http://localhost:8081/test/accept").then().statusCode(200);
//    }

//    @Test
//    public void testInvokeVertxEndpoint(io.vertx.ext.web.client.WebClient client, VertxTestContext testContext) {
//        testContext.succeeding(h -> {
//            testRequest(client, HttpMethod.POST, "/test/accept")
//                    .expect(statusCode(200))
//                    .expect(emptyResponse())
//                    .sendBuffer(io.vertx.core.buffer.Buffer.buffer("Aosdiusaoidu"), testContext)
//                    .succeeded();
//        }).handle();
//    }
    @Test
    public void testSingleService() throws Exception {
        ServiceController controller = container.getServiceController(parse("omg.wtf.whatt"));
        TransactionManager manager = container.getTransactionManager();
        manager.begin();
        controller.setMode(Mode.ACTIVE);
        manager.commit();

        waitTillServicesAre(State.UP, container.getService(parse("omg.wtf.whatt")));
        Service service = container.getService(parse("omg.wtf.whatt"));
        assertThat(service.getState()).isEqualTo(State.UP);

    }

    @Test
    public void testDependantWaiting() throws Exception {
        BatchServiceInstaller batchServiceInstaller = container.addServices();
        ServiceName dependant = parse("dependant.service");
        batchServiceInstaller.addService(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires(parse("omg.wtf.whatt"))
                .setInitialMode(Mode.ACTIVE)
                .setPayload("A Payload")
                .install();
        batchServiceInstaller.commit();
        Service service = container.getService(dependant);
        assertThat(service)
                .isNotNull();
        assertThat(service.getState())
                .isEqualTo(State.WAITING);
    }

    @Test
    public void testDependantStartsThroughDependency() throws Exception {
        BatchServiceInstaller batchServiceInstaller = container.addServices();
        ServiceName dependant = parse("dependant.service");
        batchServiceInstaller.addService(dependant)
                .setRemoteEndpoints(getMockAPI())
                .requires(parse("omg.wtf.whatt"))
                .setInitialMode(Mode.ACTIVE)
                .setPayload("A Payload")
                .install();
        batchServiceInstaller.commit();

        ServiceController dependencyController = container.getServiceController(parse("omg.wtf.whatt"));
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
        BatchServiceInstaller batchServiceInstaller = container.addServices();
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

        installService(batchServiceInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null);
        installService(batchServiceInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null);
        installService(batchServiceInstaller, c, Mode.IDLE, new ServiceName[]{f}, new ServiceName[]{a});
        installService(batchServiceInstaller, d, Mode.IDLE, new ServiceName[]{e}, new ServiceName[]{a, b});
        installService(batchServiceInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        installService(batchServiceInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c});
        installService(batchServiceInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e});
        installService(batchServiceInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b});
        installService(batchServiceInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g});
        installService(batchServiceInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h});
        batchServiceInstaller.commit();

        assertCorrectServiceRelations(container.getService(a), 0, new ServiceName[]{c, d}, null);
        assertCorrectServiceRelations(container.getService(b), 0, new ServiceName[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getService(c), 1, new ServiceName[]{f}, new ServiceName[]{a});
        assertCorrectServiceRelations(container.getService(d), 2, new ServiceName[]{e}, new ServiceName[]{a, b});
        assertCorrectServiceRelations(container.getService(e), 2, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        assertCorrectServiceRelations(container.getService(f), 1, new ServiceName[]{i}, new ServiceName[]{c});
        assertCorrectServiceRelations(container.getService(g), 1, new ServiceName[]{i, j}, new ServiceName[]{e});
        assertCorrectServiceRelations(container.getService(h), 2, new ServiceName[]{j}, new ServiceName[]{e, b});
        assertCorrectServiceRelations(container.getService(i), 2, null, new ServiceName[]{f, g});
        assertCorrectServiceRelations(container.getService(j), 2, null, new ServiceName[]{g, h});
    }

    @Test
    public void testComplexInstallationWithAlreadyExistingService() throws Exception {
        BatchServiceInstaller batchServiceInstaller = container.addServices();
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

        installService(batchServiceInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null);
        installService(batchServiceInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null);
        installService(batchServiceInstaller, c, Mode.IDLE, new ServiceName[]{f, existing}, new ServiceName[]{a});
        installService(batchServiceInstaller, d, Mode.IDLE, new ServiceName[]{e, existing}, new ServiceName[]{a, b});
        installService(batchServiceInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        installService(batchServiceInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c, existing});
        installService(batchServiceInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e});
        installService(batchServiceInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b});
        installService(batchServiceInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g});
        installService(batchServiceInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h});
        batchServiceInstaller.commit();

        assertCorrectServiceRelations(container.getService(a), 0, new ServiceName[]{c, d}, null);
        assertCorrectServiceRelations(container.getService(b), 0, new ServiceName[]{d, e, h}, null);
        assertCorrectServiceRelations(container.getService(c), 1, new ServiceName[]{f, existing}, new ServiceName[]{a});
        assertCorrectServiceRelations(container.getService(d), 2, new ServiceName[]{e, existing}, new ServiceName[]{a, b});
        assertCorrectServiceRelations(container.getService(e), 2, new ServiceName[]{g, h}, new ServiceName[]{d, b});
        assertCorrectServiceRelations(container.getService(f), 2, new ServiceName[]{i}, new ServiceName[]{c, existing});
        assertCorrectServiceRelations(container.getService(g), 1, new ServiceName[]{i, j}, new ServiceName[]{e});
        assertCorrectServiceRelations(container.getService(h), 2, new ServiceName[]{j}, new ServiceName[]{e, b});
        assertCorrectServiceRelations(container.getService(i), 2, null, new ServiceName[]{f, g});
        assertCorrectServiceRelations(container.getService(j), 2, null, new ServiceName[]{g, h});
        assertCorrectServiceRelations(container.getService(existing), 2, new ServiceName[]{f}, new ServiceName[]{c, d});
    }

    @Test
    public void testComplexWithCompletion() throws Exception {
        BatchServiceInstaller batchServiceInstaller = container.addServices();
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

        Service existingService = container.getService(existing);
        Service updatedService = existingService.toBuilder().remoteEndpoints(getMockWithStart()).payload(existing.getCanonicalName()).build();
        container.getCache().put(existing, updatedService);

        installService(batchServiceInstaller, a, Mode.IDLE, new ServiceName[]{c, d}, null, getMockWithStart(), a.getCanonicalName());
        installService(batchServiceInstaller, b, Mode.IDLE, new ServiceName[]{d, e, h}, null, getMockWithStart(), b.getCanonicalName());
        installService(batchServiceInstaller, c, Mode.IDLE, new ServiceName[]{f, existing}, new ServiceName[]{a}, getMockWithStart(), c.getCanonicalName());
        installService(batchServiceInstaller, d, Mode.IDLE, new ServiceName[]{e, existing}, new ServiceName[]{a, b}, getMockWithStart(), d.getCanonicalName());
        installService(batchServiceInstaller, e, Mode.IDLE, new ServiceName[]{g, h}, new ServiceName[]{d, b}, getMockWithStart(), e.getCanonicalName());
        installService(batchServiceInstaller, f, Mode.IDLE, new ServiceName[]{i}, new ServiceName[]{c, existing}, getMockWithStart(), f.getCanonicalName());
        installService(batchServiceInstaller, g, Mode.IDLE, new ServiceName[]{i, j}, new ServiceName[]{e}, getMockWithStart(), g.getCanonicalName());
        installService(batchServiceInstaller, h, Mode.IDLE, new ServiceName[]{j}, new ServiceName[]{e, b}, getMockWithStart(), h.getCanonicalName());
        installService(batchServiceInstaller, i, Mode.IDLE, null, new ServiceName[]{f, g}, getMockWithStart(), i.getCanonicalName());
        installService(batchServiceInstaller, j, Mode.IDLE, null, new ServiceName[]{g, h}, getMockWithStart(), j.getCanonicalName());
        batchServiceInstaller.commit();

        container.getTransactionManager().begin();
        for (ServiceName name : services) {
            container.getServiceController(name).setMode(Mode.ACTIVE);
        }
        container.getTransactionManager().commit();

        waitTillServicesAre(State.SUCCESSFUL, services);
    }

    private static void installService(BatchServiceInstaller batchServiceInstaller, ServiceName name, Mode mode, ServiceName[] dependants, ServiceName[] dependencies) {
        installService(batchServiceInstaller,name, mode, dependants, dependencies, getMockAPI(), String.format("I'm an %s!", name));
    }

    private static void installService(BatchServiceInstaller batchServiceInstaller, ServiceName name, Mode mode, ServiceName[] dependants, ServiceName[] dependencies, RemoteAPI remoteAPI, String payload) {
        ServiceBuilder builder = batchServiceInstaller.addService(name)
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

    private void assertCorrectServiceRelations(Service testing, int unfinishedDeps, ServiceName[] dependants, ServiceName[] dependencies) {
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

    private void waitTillServicesAre(State state, Service... services) {
        waitTillServicesAre(state, Arrays.stream(services).map(Service::getName).toArray(ServiceName[]::new));
    }

    private void waitTillServicesAre(State state, ServiceName... serviceNames) {
        List<ServiceName> fine = new ArrayList<>(Arrays.asList(serviceNames));
        waitSynchronouslyFor(() -> {
            Iterator<ServiceName> iterator = fine.iterator();
            while (iterator.hasNext()) {
                Service s = container.getService(iterator.next());
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