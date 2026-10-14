/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.pnc.rex.core.config.ApplicationConfig;

import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class LifecycleEvent {

    private final ApplicationConfig appConfig;

    public LifecycleEvent(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Startup(ObserverMethod.DEFAULT_PRIORITY + 1000) // last observer on start
    void start() {
        log.info("Rex instance started.");
        log.info("Instance name is '{}'.", appConfig.name());
    }

    @Shutdown(ObserverMethod.DEFAULT_PRIORITY + 1000) // last observer on shutdown
    void stop() {
        log.info("Rex instance stopped.");
    }
}
