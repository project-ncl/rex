/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class EdgeDTO {

    @NotBlank(message = "The edge source is blank")
    public String source;

    @NotBlank(message = "The target source is blank")
    public String target;
}
