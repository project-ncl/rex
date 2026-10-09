/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.counter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.ObserverMethod;

import io.quarkus.runtime.Startup;

@ApplicationScoped
public class StartupInitDefCounter {

    private final Counter maxConcurrentCounter;

    private final Counter runningCounter;

    public StartupInitDefCounter(
            @MaxConcurrent Counter maxConcurrentCounter,
            @Running Counter runningCounter) {
        this.maxConcurrentCounter = maxConcurrentCounter;
        this.runningCounter = runningCounter;
    }

    @Startup(ObserverMethod.DEFAULT_PRIORITY + 1)
    void initCounters() {
        // this triggers init of default queue's counters (if it already doesn't exist)
        maxConcurrentCounter.getMetadataValue(null);
        runningCounter.getMetadataValue(null);
    }

}
