/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model.requests;

import java.util.List;
import java.util.Set;

import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.model.Configuration;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.TransitionTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Stripped down Task model used for transition notifications.
 */
@Jacksonized
@Builder(toBuilder = true)
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinimizedTask {

    private final String name;

    private final String constraint;

    private final String correlationID;

    private final String queue;

    private final String milestoneTask;

    private final Request remoteStart;

    private final Request remoteCancel;

    private final Request remoteRollback;

    private final Request callerNotifications;

    private final State state;

    private final Set<String> dependencies;

    private final Set<String> dependants;

    private final List<ServerResponse> serverResponses;

    private final StopFlag stopFlag;

    private final String stoppedCause;

    private final Configuration configuration;

    private final List<TransitionTime> timestamps;
}