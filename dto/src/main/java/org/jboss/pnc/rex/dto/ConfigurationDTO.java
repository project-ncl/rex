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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jboss.pnc.rex.common.ConfigurationDefaults;

import java.time.Duration;
import java.util.Map;

/**
 * Class to specify metadata for a Task.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConfigurationDTO {

    public Boolean passResultsOfDependencies = ConfigurationDefaults.passResultsOfDependencies;

    public Boolean passMDCInRequestBody = ConfigurationDefaults.passMDCInRequestBody;

    public Boolean passOTELInRequestBody = ConfigurationDefaults.passOTELInRequestBody;

    public Map<String, String> mdcHeaderKeyMapping = null;

    public Duration cancelTimeout = ConfigurationDefaults.cancelTimeout;

    public Boolean delayDependantsForFinalNotification = ConfigurationDefaults.delayDependantsForFinalNotification;

    public Integer rollbackLimit = ConfigurationDefaults.rollbackLimit;

    public Boolean heartbeatEnable = ConfigurationDefaults.heartbeatEnable;

    public Duration heartbeatInitialDelay = ConfigurationDefaults.heartbeatInitialDelay;

    public Duration heartbeatInterval = ConfigurationDefaults.heartbeatInterval;

    public Integer heartbeatToleranceThreshold = ConfigurationDefaults.heartbeatToleranceThreshold;
}
