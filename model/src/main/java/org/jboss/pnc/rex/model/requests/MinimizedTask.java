package org.jboss.pnc.rex.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.ServerResponse;

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