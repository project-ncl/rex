/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.ResponseFlag;
import org.jboss.pnc.rex.common.enums.State;

import java.util.Map;
import java.util.Set;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponseDTO {

    public State state;

    public Boolean positive;

    public Integer rollbackCounter;

    public Object body;

    public Origin origin;

    public Set<ResponseFlag> flags;
}
