/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import java.time.Duration;
import java.util.Map;

import org.jboss.pnc.rex.common.ConfigurationDefaults;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
