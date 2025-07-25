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
package org.jboss.pnc.rex.core;

import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.core.config.ApplicationConfig;

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
