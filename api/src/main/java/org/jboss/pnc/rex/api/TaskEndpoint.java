/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
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

import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.rex.api.openapi.OpenapiConstants;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Set;

@Tag(name = "Task endpoint")
@Path("/rest/tasks")
public interface TaskEndpoint {

    //region MessageFormat PATH CONSTANTS
    String GET_SPECIFIC_FMT = "/%s";
    String CANCEL_PATH_FMT = "/%s/cancel";
    String GET_BY_CORRELATION_ID_FMT = "/by-correlation/%s";
    //endregion

    String TASK_ID = "Unique identifier of the task";

    @Operation(description = "This endpoint schedules graph of tasks. \n" +
            " The request has a regular graph structure with edges and vertices. \n" +
            " The tasks in edges are identified by their ID and can be either tasks EXISTING or NEW tasks referenced in vertices. " +
            " Therefore, you can add an edge between already existing tasks, new tasks or between an existing task and new task referenced in vertices. " +
            " Adding an edge where the dependant is running or has finished will result in failure. \n" +
            " The tasks in vertices have to be strictly NEW tasks and referencing EXISTING ones will result in failure. \n",
            summary = "An endpoint for starting a graph of tasks.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.CONFLICTED_CODE, description = OpenapiConstants.CONFLICTED_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Set<TaskDTO> start(@Valid @NotNull CreateGraphRequest request);

    @Operation(description = "Returns list of all tasks with optional filtering.\n " +
            "Unspecified queueFilter returns all tasks.\n" +
            "Specifying more than one queueFilter will include all tasks in those queues.\n" +
            "To filter by 'default' queue use 'null' String.\n",
            summary = "Returns list of all tasks with optional filtering.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.NO_CONTENT_CODE, description = OpenapiConstants.NO_CONTENT_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Set<TaskDTO> getAll(@BeanParam TaskFilterParameters filterParameters, @QueryParam("queue") @Nullable List<String> queueFilter);

    String GET_SPECIFIC_PATH = "/{taskID}";
    @Path(GET_SPECIFIC_PATH)
    @Operation(summary = "Returns a specific task.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.NOT_FOUND_CODE, description = OpenapiConstants.NOT_FOUND_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    TaskDTO getSpecific(@Parameter(description = TASK_ID) @PathParam("taskID") @NotBlank String taskID);

    String CANCEL_PATH = "/{taskID}/cancel";
    @Path(CANCEL_PATH)
    @Operation(summary = "Cancels execution of a task and the tasks which depend on it")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.ACCEPTED_CODE, description = OpenapiConstants.ACCEPTED_CODE),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.NOT_FOUND_CODE, description = OpenapiConstants.NOT_FOUND_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PUT
    Response cancel(@Parameter(description = TASK_ID) @PathParam("taskID") @NotBlank String taskID);

    String GET_BY_CORRELATION_ID = "/by-correlation/{correlationID}";
    @Path(GET_BY_CORRELATION_ID)
    @GET
    @Operation(summary = "Returns tasks grouped by correlation ID.")
    @APIResponses(value = {
            @APIResponse(responseCode = OpenapiConstants.SUCCESS_CODE, description = OpenapiConstants.SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.NO_CONTENT_CODE, description = OpenapiConstants.NO_CONTENT_DESCRIPTION),
            @APIResponse(responseCode = OpenapiConstants.INVALID_CODE, description = OpenapiConstants.INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.NOT_FOUND_CODE, description = OpenapiConstants.NOT_FOUND_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = OpenapiConstants.SERVER_ERROR_CODE, description = OpenapiConstants.SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Produces(MediaType.APPLICATION_JSON)
    Set<TaskDTO> byCorrelation(@PathParam("correlationID") @NotBlank String correlationID);

  /*  @Path("/{serviceName}/graph")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
            @APIResponse(responseCode = NOT_FOUND_CODE, description = NOT_FOUND_DESCRIPTION),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<TaskDTO> getGraph(@Parameter(description = SERVICE_NAME) @PathParam("serviceName") String serviceName);*/
}
