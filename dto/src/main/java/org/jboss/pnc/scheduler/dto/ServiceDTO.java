package org.jboss.pnc.scheduler.dto;

import lombok.*;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;

import java.util.HashSet;
import java.util.Set;

@Builder
public class ServiceDTO {

    private String name;

    private RemoteLinksDTO links;

    private Mode mode;

    private State state;

    private StopFlag stopFlag;

    private String payload;

    Set<String> dependants = new HashSet<>();

    Set<String> dependencies = new HashSet<>();

    public ServiceDTO(String name, RemoteLinksDTO links, Mode mode, State state, StopFlag stopFlag, String payload, Set<String> dependants, Set<String> dependencies) {
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
