/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.model.Configuration;
import org.jboss.pnc.rex.model.Request;

@Builder(toBuilder = true)
@AllArgsConstructor
@ToString
@Getter
public class InitialTask {

    private final String name;

    private final String constraint;

    private final String correlationID;

    private final String queue;

    private final String milestoneTask;

    private final Request remoteStart;

    private final Request remoteCancel;

    private final Request remoteRollback;

    private final Request callerNotifications;

    private final Mode controllerMode;

    private final Configuration configuration;
}
