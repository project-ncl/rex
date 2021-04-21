package org.jboss.pnc.scheduler.rest.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.scheduler.dto.TaskDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.dto.responses.ErrorResponse;
import org.jboss.pnc.scheduler.dto.responses.TaskListResponse;
import org.jboss.pnc.scheduler.rest.parameters.TaskFilterParameters;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.INVALID_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.INVALID_DESCRIPTION;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.NOT_FOUND_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.NOT_FOUND_DESCRIPTION;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SERVER_ERROR_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SERVER_ERROR_DESCRIPTION;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SUCCESS_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SUCCESS_DESCRIPTION;

@Tag(name = "Task endpoint")
@Path("/rest/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TaskEndpoint {

    String TASK_ID = "Unique name of the task";

/*    @Operation(summary = "Creates and starts scheduling of task")
    @APIResponses(value = {
                    @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = TaskListResponse.class))),
                    @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<TaskDTO> create(@NotNull CreateTaskRequest request);   */

    @Operation(summary = "Creates and starts scheduling of task")
    @APIResponses(value = {
                    @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = TaskListResponse.class))),
                    @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Set<TaskDTO> create(@NotNull CreateGraphRequest request);

    @Operation(summary = "Returns list of all tasks with optional filtering")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = TaskListResponse.class))),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<TaskDTO> getAll(@BeanParam TaskFilterParameters filterParameters);

    @Path("/{taskID}")
    @Operation(summary = "Gets specific task")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = TaskListResponse.class))),
            @APIResponse(responseCode = NOT_FOUND_CODE, description = NOT_FOUND_DESCRIPTION),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    TaskDTO getSpecific(@Parameter(description = TASK_ID) @PathParam("taskID") String taskID);

    @Path("/{taskID}/cancel")
    @Operation(summary = "Cancels execution of a task and it's transitively dependant parents")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = TaskListResponse.class))),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PUT
    void cancel(@Parameter(description = TASK_ID) @PathParam("taskID") String taskID);

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
