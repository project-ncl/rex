/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

    private final Request remoteStart;

    private final Request remoteCancel;

    private final Request callerNotifications;

    private final Mode controllerMode;

    private final Configuration configuration;
}
