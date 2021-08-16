package org.jboss.pnc.rex.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Request sent to the remote entity to start execution of remote Task.
 */
@Jacksonized
@Builder
@Getter
@ToString
public class StartRequest {

    private final String callback;

    private final Object payload;
}
