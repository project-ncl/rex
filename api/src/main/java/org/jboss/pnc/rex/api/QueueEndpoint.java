/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.rex.api.openapi.OpenapiConstants;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;
import org.jboss.pnc.rex.dto.responses.LongResponse;

@Tag(name = "Endpoint for queue administration")
@Path("/rest/queue")
public interface QueueEndpoint {

    String SET_CONCURRENT = "/concurrency";

    @Path(SET_CONCURRENT)
    @Operation(
            summary = "[ADMIN] Sets the amount of possible concurrent tasks from DEFAULT queue. Tasks that are currently running are never affected.")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.NO_CONTENT_CODE,
                            description = OpenapiConstants.NO_CONTENT_DESCRIPTION),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @POST
    void setConcurrent(@QueryParam("amount") @NotNull @Min(0) Long amount);

    String SET_CONCURRENT_NAMED = "/{name}/concurrency";

    @Path(SET_CONCURRENT_NAMED)
    @Operation(
            summary = "[ADMIN] Sets the amount of possible concurrent tasks in a NAMED queue. Tasks that are currently running are never affected.")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.NO_CONTENT_CODE,
                            description = OpenapiConstants.NO_CONTENT_DESCRIPTION),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @POST
    void setConcurrentNamed(@PathParam("name") String name, @QueryParam("amount") @NotNull @Min(0) Long amount);

    String GET_CONCURRENT = "/concurrency";

    @Path(GET_CONCURRENT)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns amount of possible concurrent tasks from DEFAULT queue.")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.SUCCESS_CODE,
                            description = OpenapiConstants.SUCCESS_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = LongResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GET
    LongResponse getConcurrent();

    String GET_CONCURRENT_NAMED = "/{name}/concurrency";

    @Path(GET_CONCURRENT_NAMED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns amount of possible concurrent tasks from a NAMED queue.")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.SUCCESS_CODE,
                            description = OpenapiConstants.SUCCESS_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = LongResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.NOT_FOUND_CODE,
                            description = OpenapiConstants.NOT_FOUND_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GET
    LongResponse getConcurrentNamed(@PathParam("name") String name);

    String GET_RUNNING = "/running";

    @Path(GET_RUNNING)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns amount of active tasks in queue (excluding ENQUEUED).")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.SUCCESS_CODE,
                            description = OpenapiConstants.SUCCESS_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = LongResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GET
    LongResponse getRunning();

    String GET_RUNNING_NAMED = "/{name}/running";

    @Path(GET_RUNNING_NAMED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns amount of active tasks in queue (excluding ENQUEUED).")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = OpenapiConstants.SUCCESS_CODE,
                            description = OpenapiConstants.SUCCESS_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = LongResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.INVALID_CODE,
                            description = OpenapiConstants.INVALID_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.NOT_FOUND_CODE,
                            description = OpenapiConstants.NOT_FOUND_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = OpenapiConstants.SERVER_ERROR_CODE,
                            description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GET
    LongResponse getRunningNamed(@PathParam("name") String name);
}
