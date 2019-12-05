package org.jboss.pnc.scheduler.rest.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateServiceRequest;
import org.jboss.pnc.scheduler.dto.responses.ErrorResponse;
import org.jboss.pnc.scheduler.dto.responses.ServiceListResponse;
import org.jboss.pnc.scheduler.rest.parameters.ServiceFilterParameters;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.jboss.pnc.scheduler.rest.openapi.OpenapiConstants.*;

@Tag(name = "Services endpoint")
@Path("/rest/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceEndpoint {

    String SERVICE_NAME = "Unique name of the service";

    @Operation(summary = "Creates and starts scheduling of services")
    @APIResponses(value = {
                    @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
                    @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<ServiceDTO> create(@NotNull CreateServiceRequest request);

    @Operation(summary = "Returns list of all services with optional filtering")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ServiceDTO> getAll(@BeanParam ServiceFilterParameters filterParameters);

    @Path("/{serviceName}")
    @Operation(summary = "Gets specific service")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
            @APIResponse(responseCode = NOT_FOUND_CODE, description = NOT_FOUND_DESCRIPTION),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ServiceDTO getSpecific(@Parameter(description = SERVICE_NAME) @PathParam("serviceName") String serviceName);

    @Path("/{serviceName}/cancel")
    @Operation(summary = "Cancels execution of a service and it's transitively dependant parents")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
            @APIResponse(responseCode = INVALID_CODE, description = INVALID_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PUT
    void cancel(@Parameter(description = SERVICE_NAME) @PathParam("serviceName") String serviceName);

    @Path("/{serviceName}/graph")
    @APIResponses(value = {
            @APIResponse(responseCode = SUCCESS_CODE, description = SUCCESS_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ServiceListResponse.class))),
            @APIResponse(responseCode = NOT_FOUND_CODE, description = NOT_FOUND_DESCRIPTION),
            @APIResponse(responseCode = SERVER_ERROR_CODE, description = SERVER_ERROR_DESCRIPTION,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ServiceDTO> getGraph(@Parameter(description = SERVICE_NAME) @PathParam("serviceName") String serviceName);
}
