/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.common.enums.Mode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskDTO {

    @NotBlank
    public String name;

    public String constraint;

    public String queue;

    public String milestoneTask;

    @NotNull
    public Request remoteStart;

    @NotNull
    public Request remoteCancel;

    public Request remoteRollback;

    public Request callerNotifications;

    public Mode controllerMode;

    public ConfigurationDTO configuration;
}
