package org.jboss.pnc.scheduler.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.scheduler.common.enums.State;

/**
 * Request sent to the initial caller to notify him of Task's state transitions.
 */
@Jacksonized
@Builder
@Getter
@ToString
public class NotificationRequest {

    private final State before;

    private final State after;

    private final MinimizedTask task;

    private final Object attachment;
}
