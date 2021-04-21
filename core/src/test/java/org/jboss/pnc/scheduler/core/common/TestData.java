package org.jboss.pnc.scheduler.core.common;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.jboss.pnc.scheduler.dto.EdgeDTO;
import org.jboss.pnc.scheduler.dto.RemoteLinksDTO;
import org.jboss.pnc.scheduler.dto.requests.CreateGraphRequest;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.rest.parameters.TaskFilterParameters;

public class TestData {

    public static CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode) {
        return getMockTask(name, mode, getMockDTOAPI(), String.format("I'm an %s!", name));
    }

    public static CreateTaskDTO getMockTaskWithStart(String name, Mode mode) {
        return getMockTask(name, mode, getStartingMockDTOAPI(), name);
    }

    public static CreateGraphRequest getSingleWithoutStart(String name) {
        return CreateGraphRequest.builder()
                .vertex(name, CreateTaskDTO.builder()
                        .name(name)
                        .controllerMode(Mode.ACTIVE)
                        .remoteLinks(getMockDTOAPI())
                        .payload("{id: 100}")
                        .build())
                .build();
    }

    public static CreateTaskDTO getMockTask(String name, Mode mode, RemoteLinksDTO remoteLinks, String payload) {
        return CreateTaskDTO.builder()
                .name(name)
                .controllerMode(mode)
                .remoteLinks(remoteLinks)
                .payload(payload)
                .build();
    }

    public static RemoteLinksDTO getMockDTOAPI() {
        return RemoteLinksDTO.builder()
                .startUrl("http://localhost:8081/test/accept")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }

    public static RemoteLinksDTO getStartingMockDTOAPI() {
        return RemoteLinksDTO.builder()
                .startUrl("http://localhost:8081/test/acceptAndStart")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }

    public static RemoteAPI getMockWithStart() {
        return RemoteAPI.builder()
                .startUrl("http://localhost:8081/test/acceptAndStart")
                .stopUrl("http://localhost:8081/test/stop")
                .build();
    }

    public static TaskFilterParameters getAllParameters() {
        TaskFilterParameters params = new TaskFilterParameters();
        params.setFinished(true);
        params.setRunning(true);
        params.setWaiting(true);
        return params;
    }

    public static CreateGraphRequest getComplexGraph(boolean withStart) {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";
        String e = "e";
        String f = "f";
        String g = "g";
        String h = "h";
        String i = "i";
        String j = "j";
        return CreateGraphRequest.builder()
                .edge(new EdgeDTO(c, a))
                .edge(new EdgeDTO(d, a))
                .edge(new EdgeDTO(d, b))
                .edge(new EdgeDTO(e, d))
                .edge(new EdgeDTO(e, b))
                .edge(new EdgeDTO(f, c))
                .edge(new EdgeDTO(g, e))
                .edge(new EdgeDTO(h, e))
                .edge(new EdgeDTO(h, b))
                .edge(new EdgeDTO(i, f))
                .edge(new EdgeDTO(i, g))
                .edge(new EdgeDTO(j, g))
                .edge(new EdgeDTO(j, h))
                .vertex(a, withStart ? getMockTaskWithStart(a, Mode.ACTIVE) : getMockTaskWithoutStart(a, Mode.IDLE))
                .vertex(b, withStart ? getMockTaskWithStart(b, Mode.ACTIVE) : getMockTaskWithoutStart(b, Mode.IDLE))
                .vertex(c, withStart ? getMockTaskWithStart(c, Mode.ACTIVE) : getMockTaskWithoutStart(c, Mode.IDLE))
                .vertex(d, withStart ? getMockTaskWithStart(d, Mode.ACTIVE) : getMockTaskWithoutStart(d, Mode.IDLE))
                .vertex(e, withStart ? getMockTaskWithStart(e, Mode.ACTIVE) : getMockTaskWithoutStart(e, Mode.IDLE))
                .vertex(f, withStart ? getMockTaskWithStart(f, Mode.ACTIVE) : getMockTaskWithoutStart(f, Mode.IDLE))
                .vertex(g, withStart ? getMockTaskWithStart(g, Mode.ACTIVE) : getMockTaskWithoutStart(g, Mode.IDLE))
                .vertex(h, withStart ? getMockTaskWithStart(h, Mode.ACTIVE) : getMockTaskWithoutStart(h, Mode.IDLE))
                .vertex(i, withStart ? getMockTaskWithStart(i, Mode.ACTIVE) : getMockTaskWithoutStart(i, Mode.IDLE))
                .vertex(j, withStart ? getMockTaskWithStart(j, Mode.ACTIVE) : getMockTaskWithoutStart(j, Mode.IDLE))
                .build();
    }
}
