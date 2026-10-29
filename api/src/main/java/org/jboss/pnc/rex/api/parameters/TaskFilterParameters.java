/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.api.parameters;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.Data;

@Data
public class TaskFilterParameters {

    @Parameter(description = "Should include running tasks?")
    @QueryParam("running")
    @DefaultValue("false")
    private Boolean running;

    @Parameter(description = "Should include waiting tasks?")
    @QueryParam("waiting")
    @DefaultValue("false")
    private Boolean waiting;

    @Parameter(description = "Should include finished tasks?")
    @QueryParam("finished")
    @DefaultValue("false")
    private Boolean finished;

    @Parameter(description = "Should include task in process of rollback?")
    @QueryParam("rollingback")
    @DefaultValue("false")
    private Boolean rollingback;

}
