package org.jboss.pnc.scheduler.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.scheduler.common.enums.State;

@Jacksonized
@Builder
@Getter
public class NotificationRequest {

    private final State before;

    private final State after;

    private final MinimizedTask task;

    private final Object attachment;
}
