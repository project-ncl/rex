package org.jboss.pnc.scheduler.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.model.Request;
import org.jboss.pnc.scheduler.model.ServerResponse;

import java.util.List;
import java.util.Set;

/**
 * Stripped down Task model used for transition notifications.
 */
@Jacksonized
@Builder
@Getter
@ToString
public class MinimizedTask {

    private final String name;

    private final Request remoteStart;

    private final Request remoteCancel;

    private final Request callerNotifications;

    private final State state;

    private final Set<String> dependencies;

    private final Set<String> dependants;

    private final List<ServerResponse> serverResponses;

    private final StopFlag stopFlag;
}