/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import java.util.Map;

/**
 * Runs the entire {@link QueueTest} suite with the Infinispan StrongCounter running-counter tracker
 * active ({@code rex.queue.running-tracker=strong}), validating the reserve-then-compensate
 * admission path and per-queue isolation against a real Infinispan counter subsystem.
 * <p>
 * Test isolation holds because {@code MaintenanceEndpoint} CLEAR_ALL (run before each test) calls
 * {@code QueueManager#synchronizeRunningCounter()}, which reconciles every known running counter —
 * including StrongCounters — down to the live task count (zero after the caches are cleared).
 */
@QuarkusTest
@TestProfile(StrongCounterQueueTest.StrongTrackerProfile.class)
public class StrongCounterQueueTest extends QueueTest {

    public static class StrongTrackerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("rex.queue.running-tracker", "strong");
        }
    }
}
