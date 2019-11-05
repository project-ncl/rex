package org.jboss.pnc.scheduler.core;

import lombok.Getter;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.ServiceBuilder;
import org.jboss.pnc.scheduler.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ServiceBuilderImpl implements ServiceBuilder, Comparable<ServiceBuilderImpl>{

    private static final Logger logger = LoggerFactory.getLogger(ServiceBuilderImpl.class);

    @Getter
    private BatchServiceInstallerImpl installer;

    @Getter
    private final ServiceName name;

    @Getter
    private Set<ServiceName> dependencies = new HashSet<>();

    @Getter
    private Set<ServiceName> dependants = new HashSet<>();

    @Getter
    private String payload;

    @Getter
    private boolean installed = false;

    @Getter
    private Mode initialMode = Mode.IDLE;

    public ServiceBuilderImpl(ServiceName name, BatchServiceInstallerImpl installer) {
        this.installer = assertNotNull(installer);
        this.name = assertNotNull(name);
    }

    @Override
    public ServiceBuilder requires(ServiceName dependency) {
        assertNotInstalled();
        assertNotNull(dependency);
        assertNotItself(dependency);

        dependencies.add(dependency);
        return this;
    }

    @Override
    public ServiceBuilder isRequiredBy(ServiceName dependant) {
        assertNotInstalled();
        assertNotNull(dependant);
        assertNotItself(dependant);

        dependants.add(dependant);
        return this;
    }

    @Override
    public ServiceBuilder setInitialMode(Mode mode) {
        assertNotInstalled();
        assertNotCancelledState(mode);
        assertNotNull(mode);

        this.initialMode = mode;
        return this;
    }

    @Override
    public ServiceBuilder setPayload(String payload) {
        assertNotInstalled();

        this.payload = payload;
        return this;
    }



    @Override
    public void install() {
        assertNotInstalled();

        installed = true;
        installer.commitBuilder(this);
    }

    public Service toPartiallyFilledService() {
        assertAlreadyInstalled();

        return Service.builder()
                .name(getName())
                .dependants(getDependants())
                .dependencies(getDependencies())
                .unfinishedDependencies(0)
                .controllerMode(initialMode)
                .payload(payload)
                .stopFlag(StopFlag.NONE)
                .remoteEndpoints(RemoteAPI.builder()
                        .startUrl("hello.url") //TODO FIXME
                        .stopUrl("bye.url")
                        .build())
                .state(State.NEW)
                .build();
    }

    private void assertAlreadyInstalled() {
        if (!installed) {
            throw new IllegalStateException("Builder is not installed, install it to use this method.");
        }
    }

    private void assertNotInstalled() {
        if (installed) {
            throw new IllegalStateException("Builder is already installed, please refrain from using it.");
        }
    }

    private void assertNotCancelledState(Mode mode) {
        if (mode.equals(Mode.CANCEL)) {
            throw new IllegalArgumentException("Service cannot with cancelled mode.");
        }
    }

    private <T> T assertNotNull(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Parameter of class: "+ object.getClass().getCanonicalName() + " cannot be null.");
        }
        return object;
    }

    private void assertNotItself(ServiceName serviceName) {
        if (serviceName.equals(name)) {
            throw new IllegalArgumentException("Service cannot depend/be dependent on itself.");
        }
    }

    @Override
    public int compareTo(ServiceBuilderImpl builder) {
        if (builder.getDependants().contains(name)) {
            return -1;
        }
        if (builder.getDependencies().contains(name)) {
            return 1;
        }
        return 0;
    }
}
