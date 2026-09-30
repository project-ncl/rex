/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ConfigurationDefaults {
    public static final boolean passResultsOfDependencies = false;
    public static final boolean passMDCInRequestBody = false;
    public static final boolean passOTELInRequestBody = false;
    public static final Duration cancelTimeout = Duration.ZERO;
    public static final boolean delayDependantsForFinalNotification = false;
    public static final int rollbackLimit = 3;
    public static final boolean heartbeatEnable = false;
    public static final Duration heartbeatInitialDelay = Duration.ZERO;
    public static final Duration heartbeatInterval = Duration.of(5, ChronoUnit.SECONDS);
    public static final int heartbeatToleranceThreshold = 3;
}
