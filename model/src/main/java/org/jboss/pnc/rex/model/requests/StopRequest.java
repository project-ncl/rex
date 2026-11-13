/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model.requests;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Request sent to the remote entity to cancel execution of remote Task.
 */
@Jacksonized
@Builder
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StopRequest {

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

    private final Object payload;

    private final Map<String, String> mdc;

    private final Map<String, Object> taskResults;
}
