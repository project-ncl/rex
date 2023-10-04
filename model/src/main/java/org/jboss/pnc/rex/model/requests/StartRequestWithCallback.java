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
package org.jboss.pnc.rex.model.requests;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request sent to the remote entity to start execution of remote Task, with extra fields for callback endpoints
 */
@Jacksonized
@SuperBuilder
@Getter
@ToString
public class StartRequestWithCallback extends StartRequest {
    /**
     * The referenced endpoint is application/JSON only and serves for both positive and negative callback.
     * Additionally, it uses a specific JSON body
     *
     * Don't use it
     */
    @Deprecated
    private final org.jboss.pnc.api.dto.Request callback;

    /**
     * The referenced endpoint is generic and serves for positive callback.
     */
    private final org.jboss.pnc.api.dto.Request positiveCallback;

    /**
     * The referenced endpoint is generic and serves for negative callback.
     */
    private final org.jboss.pnc.api.dto.Request negativeCallback;
}