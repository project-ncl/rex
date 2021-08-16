package org.jboss.pnc.rex.core.common;

import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.HeaderDTO;
import org.jboss.pnc.rex.dto.HttpRequest;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.rest.parameters.TaskFilterParameters;

import java.util.List;

public class TestData {

    public static CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode) {
        return getMockTaskWithoutStart(name, mode, false);
    }
    public static CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode, boolean withNotifications) {
        String payload = String.format("I'm an %s!", name);
        if (withNotifications) {
            return getMockTask(name, mode, getRequestWithoutStart(payload), getStopRequest(payload), getNotificationsRequest());
        }
        return getMockTask(name, mode, getRequestWithoutStart(payload), getStopRequest(payload), null);
    }

    public static CreateTaskDTO getMockTaskWithStart(String name, Mode mode) {
        return getMockTaskWithStart(name, mode, false);
    }

    public static CreateTaskDTO getMockTaskWithStart(String name, Mode mode, boolean withNotifications) {
        if (withNotifications) {
            return getMockTask(name, mode, getRequestWithStart(name), getStopRequest(name), getNotificationsRequest());
        }
        return getMockTask(name, mode, getRequestWithStart(name), getStopRequest(name), null);
    }

    public static CreateGraphRequest getSingleWithoutStart(String name) {
        return CreateGraphRequest.builder()
                .vertex(name, CreateTaskDTO.builder()
                        .name(name)
                        .controllerMode(Mode.ACTIVE)
                        .remoteStart(getRequestWithoutStart("{id: 100}"))
                        .remoteCancel(getStopRequest("{id: 100}"))
                        .build())
                .build();
    }

    public static CreateTaskDTO getMockTask(String name, Mode mode, HttpRequest startRequest, HttpRequest stopRequest, HttpRequest notificationsRequest) {
        return CreateTaskDTO.builder()
                .name(name)
                .controllerMode(mode)
                .remoteStart(startRequest)
                .remoteCancel(stopRequest)
                .callerNotifications(notificationsRequest)
                .build();
    }

    public static HttpRequest getRequestWithoutStart(String payload) {
        return HttpRequest.builder()
                .url("http://localhost:8081/test/accept")
                .method(Method.POST)
                .headers(List.of(HeaderDTO.builder()
                        .name("Content-Type")
                        .value("application/json")
                        .build()))
                .attachment(payload)
                .build();
    }

    public static HttpRequest getStopRequest(String payload) {
        return HttpRequest.builder()
                .url("http://localhost:8081/test/stop")
                .method(Method.POST)
                .headers(List.of(HeaderDTO.builder()
                        .name("Content-Type")
                        .value("application/json")
                        .build()))
                .attachment(payload)
                .build();
    }

    public static HttpRequest getRequestWithStart(String payload) {
        return HttpRequest.builder()
                .url("http://localhost:8081/test/acceptAndStart")
                .method(Method.POST)
                .headers(List.of(HeaderDTO.builder()
                        .name("Content-Type")
                        .value("application/json")
                        .build()))
                .attachment(payload)
                .build();
    }

    public static HttpRequest getNotificationsRequest() {
        return HttpRequest.builder()
                .method(Method.POST)
                .headers(List.of(HeaderDTO.builder()
                        .name("Content-Type")
                        .value("application/json")
                        .build()))
                .attachment("hello")
                .url("http://localhost:8081/transition/record")
                .build();
    }

    public static Request getEndpointWithStart(String payload) {
        return Request.builder()
                .url("http://localhost:8081/test/acceptAndStart")
                .method(Method.POST)
                .headers(List.of(Header.builder()
                        .name("Content-Type")
                        .value("application/json")
                        .build()))
                .attachment(payload)
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
        return getComplexGraph(withStart, false);
    }

    public static CreateGraphRequest getComplexGraph(boolean withStart, boolean withNotifications) {
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
                .vertex(a, withStart ? getMockTaskWithStart(a, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(a, Mode.IDLE, withNotifications))
                .vertex(b, withStart ? getMockTaskWithStart(b, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(b, Mode.IDLE, withNotifications))
                .vertex(c, withStart ? getMockTaskWithStart(c, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(c, Mode.IDLE, withNotifications))
                .vertex(d, withStart ? getMockTaskWithStart(d, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(d, Mode.IDLE, withNotifications))
                .vertex(e, withStart ? getMockTaskWithStart(e, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(e, Mode.IDLE, withNotifications))
                .vertex(f, withStart ? getMockTaskWithStart(f, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(f, Mode.IDLE, withNotifications))
                .vertex(g, withStart ? getMockTaskWithStart(g, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(g, Mode.IDLE, withNotifications))
                .vertex(h, withStart ? getMockTaskWithStart(h, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(h, Mode.IDLE, withNotifications))
                .vertex(i, withStart ? getMockTaskWithStart(i, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(i, Mode.IDLE, withNotifications))
                .vertex(j, withStart ? getMockTaskWithStart(j, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(j, Mode.IDLE, withNotifications))
                .build();
    }

    public static CreateGraphRequest getComplexGraphWithoutEnd(boolean withStart, boolean withNotifications) {
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
                .vertex(a, withStart ? getMockTaskWithoutStart(a, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(a, Mode.IDLE, withNotifications))
                .vertex(b, withStart ? getMockTaskWithoutStart(b, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(b, Mode.IDLE, withNotifications))
                .vertex(c, withStart ? getMockTaskWithoutStart(c, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(c, Mode.IDLE, withNotifications))
                .vertex(d, withStart ? getMockTaskWithoutStart(d, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(d, Mode.IDLE, withNotifications))
                .vertex(e, withStart ? getMockTaskWithoutStart(e, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(e, Mode.IDLE, withNotifications))
                .vertex(f, withStart ? getMockTaskWithoutStart(f, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(f, Mode.IDLE, withNotifications))
                .vertex(g, withStart ? getMockTaskWithoutStart(g, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(g, Mode.IDLE, withNotifications))
                .vertex(h, withStart ? getMockTaskWithoutStart(h, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(h, Mode.IDLE, withNotifications))
                .vertex(i, withStart ? getMockTaskWithoutStart(i, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(i, Mode.IDLE, withNotifications))
                .vertex(j, withStart ? getMockTaskWithoutStart(j, Mode.ACTIVE, withNotifications) : getMockTaskWithoutStart(j, Mode.IDLE, withNotifications))
                .build();
    }
}
