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
package org.jboss.pnc.rex.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.model.Configuration;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.TransitionTime;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stripped down Task model used for transition notifications.
 */
@Jacksonized
@Builder(toBuilder = true)
@Getter
@ToString
public class MinimizedTask {

    private final String name;

    private final String constraint;

    private final String correlationID;

    private final Request remoteStart;

    private final Request remoteCancel;

    private final Request callerNotifications;

    private final State state;

    private final Set<String> dependencies;

    private final Set<String> dependants;

    private final List<ServerResponse> serverResponses;

    private final StopFlag stopFlag;

    private final Configuration configuration;

    private final List<TransitionTime> timestamps;
}