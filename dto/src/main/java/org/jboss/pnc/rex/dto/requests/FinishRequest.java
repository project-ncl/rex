/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Deprecated
public class FinishRequest {

    @NotNull
    public Boolean status;

    public Object response;
}
