package org.jboss.pnc.scheduler.dto;

import lombok.*;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;

import java.util.Set;

@Data
public class ServiceDTO {

    private String name;

    private RemoteLinksDTO links;

    private Mode mode;

    private State state;

    private StopFlag stopFlag;

    private String payload;

    Set<String> dependants;

    Set<String> dependencies;
}
