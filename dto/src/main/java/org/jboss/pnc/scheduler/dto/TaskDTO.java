package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;

import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    public String name;

    public HttpRequest remoteStart;

    public HttpRequest remoteCancel;

    public HttpRequest callerNotifications;

    public State state;

    public StopFlag stopFlag;

    public Set<String> dependants = new HashSet<>();

    public Set<String> dependencies = new HashSet<>();
}
