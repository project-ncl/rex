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
package org.jboss.pnc.rex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to specify metadata for a Task.
 */
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
@Slf4j
@Jacksonized
public class Configuration {

    /**
     * Specify whether we want to pass results of direct dependencies in the StartRequest and StopRequest
     */
    @Getter(onMethod_ = {@ProtoField(number = 1, defaultValue = "" + Defaults.passResultsOfDependencies)})
    private final boolean passResultsOfDependencies;

    /**
     * Specify whether to put applied MDC values into the request body. MDCHeaderKeyMapping MUST be configured.
     */
    @Getter(onMethod_ = {@ProtoField(number = 2, defaultValue = "" + Defaults.passMDCInRequestBody)})
    private final boolean passMDCInRequestBody;

    /**
     * Specify whether to put OTEL values into the request body. MDCHeaderKeyMapping MUST be configured.
     */
    @Getter(onMethod_ = {@ProtoField(number = 3, defaultValue = "" + Defaults.passOTELInRequestBody)})
    private final boolean passOTELInRequestBody;

    /**
     * If configured, Rex looks at a Request headers and extracts values and maps them into it's MDC.
     *
     * (Header Key -> MDC Key)
     *
     * Has to be HashMap for ISPN inference in AllArgsConstructor
     */
    private final HashMap<String, String> mdcHeaderKeyMapping;

    @ProtoField(number = 4, javaType = HashMap.class)
    public Map<String, String> getMdcHeaderKeyMapping() {
        return mdcHeaderKeyMapping;
    }

    @Getter(onMethod_ = {@ProtoField(number = 5)})
    private final Duration cancelTimeout;

    /**
     * If configured, dependants have delayed start and kept in waiting state until the final notification of this task
     * is finished. The resulting status code of the notification determines whether dependants start or fail. Meaning,
     * even if the Task is in state SUCCESS, 5xx or 4xx on the notification means that dependant tasks are signaled to
     * FAIL (see StopFlag.DEPENDENCY_NOTIFICATION_FAILED).
     */
    @Getter(onMethod_ = {@ProtoField(number = 6, defaultValue = "" + Defaults.delayDependantsForFinalNotification)})
    private final boolean delayDependantsForFinalNotification;

    public static class Defaults {
        public static final boolean passResultsOfDependencies = false;
        public static final boolean passMDCInRequestBody = false;
        public static final boolean passOTELInRequestBody = false;
        public static final Duration cancelTimeout = Duration.ZERO;
        public static final String cancelTimeoutString = "PT0S";
        public static final boolean delayDependantsForFinalNotification = false;
    }
}
