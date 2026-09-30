/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class TaskGraph {

    @Singular
    private final Map<String, InitialTask> vertices;

    @Singular
    private final Set<Edge> edges;
}
