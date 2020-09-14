package org.jboss.pnc.scheduler.core;

import lombok.Getter;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.core.api.TaskBuilder;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class TaskBuilderImpl implements TaskBuilder, Comparable<TaskBuilderImpl>{

    private static final Logger logger = LoggerFactory.getLogger(TaskBuilderImpl.class);

    @Getter
    private BatchTaskInstallerImpl installer;

    @Getter
    private final String name;

    @Getter
    private Set<String> dependencies = new HashSet<>();

    @Getter
    private Set<String> dependants = new HashSet<>();

    @Getter
    private String payload;

    @Getter
    private RemoteAPI remoteAPI;

    @Getter
    private boolean installed = false;

    @Getter
    private Mode initialMode = Mode.IDLE;

    public TaskBuilderImpl(String name, BatchTaskInstallerImpl installer) {
        this.installer = assertNotNull(installer);
        this.name = assertNotNull(name);
    }

    @Override
    public TaskBuilder requires(String dependency) {
        assertNotInstalled();
        assertNotNull(dependency);
        assertNotItself(dependency);

        dependencies.add(dependency);
        return this;
    }

    @Override
    public TaskBuilder isRequiredBy(String dependant) {
        assertNotInstalled();
        assertNotNull(dependant);
        assertNotItself(dependant);

        dependants.add(dependant);
        return this;
    }

    @Override
    public TaskBuilder setInitialMode(Mode mode) {
        assertNotInstalled();
        assertNotCancelledState(mode);
        assertNotNull(mode);

        this.initialMode = mode;
        return this;
    }

    @Override
    public TaskBuilder setRemoteEndpoints(RemoteAPI api) {
        assertNotInstalled();
        assertNotNull(api);
        assertValidUrls(api);

        this.remoteAPI = api;
        return this;
    }

    private void assertValidUrls(RemoteAPI api) {
        assertNotNull(api.getStartUrl());
        assertNotNull(api.getStopUrl());
        try {
            URI start = new URI(api.getStartUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("startUrl:" + api.getStartUrl() + " is not valid url");
        }
        try {
            URI stop = new URI(api.getStopUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("stopUrl:" + api.getStartUrl() + " is not a valid url");
        }


    }

    @Override
    public TaskBuilder setPayload(String payload) {
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

    public Task toPartiallyFilledTask() {
        assertAlreadyInstalled();

        return Task.builder()
                .name(getName())
                .dependants(getDependants())
                .dependencies(getDependencies())
                .unfinishedDependencies(0)
                .controllerMode(Mode.IDLE)
                .payload(payload)
                .stopFlag(StopFlag.NONE)
                .remoteEndpoints(remoteAPI)
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
            throw new IllegalArgumentException("Task cannot with cancelled mode.");
        }
    }

    private <T> T assertNotNull(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Parameter of class: "+ object.getClass().getCanonicalName() + " cannot be null.");
        }
        return object;
    }

    private void assertNotItself(String serviceName) {
        if (serviceName.equals(name)) {
            throw new IllegalArgumentException("Task cannot depend/be dependent on itself.");
        }
    }

    @Override
    public int compareTo(TaskBuilderImpl builder) {
        if (builder.getDependants().contains(name)) {
            return -1;
        }
        if (builder.getDependencies().contains(name)) {
            return 1;
        }
        return 0;
    }
}
