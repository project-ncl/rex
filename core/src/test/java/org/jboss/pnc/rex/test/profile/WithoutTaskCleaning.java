/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.profile;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class WithoutTaskCleaning implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("scheduler.options.task-configuration.clean", "false");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
