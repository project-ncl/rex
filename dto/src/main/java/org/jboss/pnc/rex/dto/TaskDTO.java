/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TaskDTO {

    public String name;

    public String constraint;

    public String queue;

    public String correlationID;

    public Request remoteStart;

    public Request remoteCancel;

    public Request callerNotifications;

    public Request remoteRollback;

    public State state;

    public StopFlag stopFlag;

    public String stoppedCause;

    public List<ServerResponseDTO> serverResponses = new ArrayList<>();

    public Set<String> dependants = new TreeSet<>();

    public Set<String> dependencies = new TreeSet<>();

    public ConfigurationDTO configuration = new ConfigurationDTO();

    /**
     * The list of timestamps is order from the earliest transitions
     */
    public List<TransitionTimeDTO> timestamps = new ArrayList<>();

    public String milestoneTask;
}
