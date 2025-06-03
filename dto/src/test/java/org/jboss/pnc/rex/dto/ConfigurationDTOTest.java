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
package org.jboss.pnc.rex.dto;

import org.jboss.pnc.rex.common.ConfigurationDefaults;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationDTOTest {

    @Test
    void testDefaultsApply() {
        ConfigurationDTO config = ConfigurationDTO.builder().build();
        assertEquals(ConfigurationDefaults.passResultsOfDependencies, config.passResultsOfDependencies);
        assertEquals(ConfigurationDefaults.passMDCInRequestBody, config.passMDCInRequestBody);
        assertEquals(ConfigurationDefaults.passOTELInRequestBody, config.passOTELInRequestBody);
        assertEquals(ConfigurationDefaults.cancelTimeout, config.cancelTimeout);
        assertEquals(ConfigurationDefaults.delayDependantsForFinalNotification, config.delayDependantsForFinalNotification);
        assertEquals(ConfigurationDefaults.rollbackLimit, config.rollbackLimit);
        assertEquals(ConfigurationDefaults.heartbeatEnable, config.heartbeatEnable);
        assertEquals(ConfigurationDefaults.heartbeatInitialDelay, config.heartbeatInitialDelay);
        assertEquals(ConfigurationDefaults.heartbeatInterval, config.heartbeatInterval);
        assertEquals(ConfigurationDefaults.heartbeatToleranceThreshold, config.heartbeatToleranceThreshold);
    }

    @Test
    void testAbleToChangeDefaults() {
        Duration duration = Duration.ofDays(420);
        ConfigurationDTO config = ConfigurationDTO.builder().cancelTimeout(duration).build();
        assertEquals(duration, config.getCancelTimeout());
    }
}