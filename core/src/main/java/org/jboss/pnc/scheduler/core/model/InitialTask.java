package org.jboss.pnc.scheduler.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.model.RemoteAPI;

@Builder
@AllArgsConstructor
public class InitialTask {

    @Getter
    private final String name;

    @Getter
    private final RemoteAPI remoteEndpoints;

    @Getter
    private final String payload;

    @Getter
    private final Mode controllerMode;
}
