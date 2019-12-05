package org.jboss.pnc.scheduler.rest.parameters;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

@Data
public class ServiceFilterParameters {

    @Parameter(description = "Should include running services?")
    @QueryParam("running")
    @DefaultValue("false")
    private Boolean running;

    @Parameter(description = "Should include waiting services?")
    @QueryParam("waiting")
    @DefaultValue("false")
    private Boolean waiting;

    @Parameter(description = "Should include finished services?")
    @QueryParam("finished")
    @DefaultValue("false")
    private Boolean finished;

}
