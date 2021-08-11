package org.jboss.pnc.scheduler.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Request sent to the remote entity to cancel execution of remote Task.
 */
@Jacksonized
@Builder
@Getter
@ToString
public class StopRequest {

    private final String callback;

    private final Object payload;
}
