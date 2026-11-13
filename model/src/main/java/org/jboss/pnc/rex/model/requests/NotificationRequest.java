/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model.requests;

import org.jboss.pnc.rex.common.enums.State;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Request sent to the initial caller to notify him of Task's state transitions.
 */
@Jacksonized
@Builder
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequest {

    private final State before;

    private final State after;

    private final MinimizedTask task;

    private final Object attachment;
}
