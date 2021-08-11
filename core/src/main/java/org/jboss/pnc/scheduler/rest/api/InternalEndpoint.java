package org.jboss.pnc.scheduler.rest.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.scheduler.dto.requests.FinishRequest;
import org.jboss.pnc.scheduler.dto.responses.ErrorResponse;
import org.jboss.pnc.scheduler.dto.responses.LongResponse;

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

import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.INVALID_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.INVALID_DESCRIPTION;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SERVER_ERROR_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SERVER_ERROR_DESCRIPTION;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SUCCESS_CODE;
import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.SUCCESS_DESCRIPTION;

@Tag(name = "Internal endpoint")
@Path("/rest/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InternalEndpoint {

    @Path("/{taskName}/finish")
    @Operation(summary = "[ADMIN] Used by remote entity to report Task completion.")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    void finish(@PathParam("taskName") @NotEmpty String taskName, @Valid @NotNull FinishRequest result);

    @Path("/options/concurrency")
    @Operation(summary = "[ADMIN] Sets the amount of possible concurrent builds. Tasks that are currently running are never affected.")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    void setConcurrent(@QueryParam("amount") @NotNull @Min(0) Long amount);

    @Path("/options/concurrency")
    @Operation(summary = "Returns amount of possible concurrent builds.")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = LongResponse.class))),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    LongResponse getConcurrent();
}
