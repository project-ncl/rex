package org.jboss.pnc.scheduler.dto;

import lombok.Builder;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;

import java.util.HashSet;
import java.util.Set;

@Builder
public class TaskDTO {

    public String name;

    public RemoteLinksDTO links;

    public Mode mode;

    public State state;

    public StopFlag stopFlag;

    public String payload;

    public Set<String> dependants = new HashSet<>();

    public Set<String> dependencies = new HashSet<>();

    public TaskDTO() {
    }

    public TaskDTO(String name, RemoteLinksDTO links, Mode mode, State state, StopFlag stopFlag, String payload, Set<String> dependants, Set<String> dependencies) {
        if (dependants == null) {
            dependants = new HashSet<>();
        }
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }
        this.name = name;
        this.links = links;
        this.mode = mode;
        this.state = state;
        this.stopFlag = stopFlag;
        this.payload = payload;
        this.dependants = dependants;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public RemoteLinksDTO getLinks() {
        return links;
    }

    public Mode getMode() {
        return mode;
    }

    public State getState() {
        return state;
    }

    public StopFlag getStopFlag() {
        return stopFlag;
    }

    public String getPayload() {
        return payload;
    }

    public Set<String> getDependants() {
        return dependants;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }
}
