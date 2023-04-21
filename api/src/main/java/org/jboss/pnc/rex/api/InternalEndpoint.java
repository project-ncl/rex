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
package org.jboss.pnc.rex.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.rex.api.openapi.OpenapiConstants;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;
import org.jboss.pnc.rex.dto.responses.LongResponse;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Tag(name = "Internal endpoint")
@Path("/rest/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RestClient
@RegisterClientHeaders
public interface InternalEndpoint {

    @Path("/{taskName}/finish")
    @Operation(summary = "[ADMIN] Used by remote entity to report Task completion.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    void finish(@PathParam("taskName") @NotEmpty String taskName, @Valid @NotNull FinishRequest result);

    @Path("/options/concurrency")
    @Operation(summary = "[ADMIN] Sets the amount of possible concurrent builds. Tasks that are currently running are never affected.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    void setConcurrent(@QueryParam("amount") @NotNull @Min(0) Long amount);

    @Path("/options/concurrency")
    @Operation(summary = "Returns amount of possible concurrent builds.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = LongResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    LongResponse getConcurrent();
}
