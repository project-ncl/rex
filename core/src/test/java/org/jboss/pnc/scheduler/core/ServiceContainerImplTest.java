package org.jboss.pnc.scheduler.core;

import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.pnc.scheduler.core.api.BatchServiceInstaller;
import org.jboss.pnc.scheduler.core.model.Mode;
import org.jboss.pnc.scheduler.core.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.msc.service.ServiceName.parse;

@QuarkusTest
class ServiceContainerImplTest {

    private static final Logger log = LoggerFactory.getLogger(ServiceContainerImplTest.class);

    @Inject
    private ServiceContainerImpl container;

    @BeforeEach
    public void before() throws Exception {
        container.getCache().clear();
        BatchServiceInstaller installer = container.addServices();
        installer
                .addService(parse("omg.wtf.whatt"))
                .setPayload("{id: 100}")
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
}