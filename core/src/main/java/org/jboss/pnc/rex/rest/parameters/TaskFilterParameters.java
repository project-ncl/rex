package org.jboss.pnc.rex.rest.parameters;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

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

}
