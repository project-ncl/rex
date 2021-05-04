package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.common.enums.Mode;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskDTO {

    public String name;

    public HttpRequest remoteStart;

    public HttpRequest remoteCancel;

    public HttpRequest callerNotifications;

    public Mode controllerMode;
}
