package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.common.enums.Mode;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class CreateTaskDTO {

    public String name;

    public RemoteLinksDTO remoteLinks;

    public String payload;

    public Mode controllerMode;
}
