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
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
