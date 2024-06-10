/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.test.common;

import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.common.enums.Method;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.Header;
import org.jboss.pnc.rex.model.Request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestData {

    public static CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode) {
        return getMockTaskWithoutStart(name, mode, false);
    }

    public static CreateTaskDTO getMockTaskWithoutStart(String name, Mode mode, boolean withNotifications) {
        String payload = String.format("I'm an %s!", name);
        if (withNotifications) {
            return createMockTask(name, mode, getRequestWithoutStart(payload), getStopRequestWithCallback(payload), getNotificationsRequest());
        }
        return createMockTask(name, mode, getRequestWithoutStart(payload), getStopRequestWithCallback(payload), null);
    }

    public static CreateTaskDTO getMockTaskWithStart(String name, Mode mode) {
        return getMockTaskWithStart(name, mode, false);
    }

    public static CreateTaskDTO getMockTaskWithStart(String name, Mode mode, boolean withNotifications) {
        if (withNotifications) {
            return createMockTask(name, mode, getRequestWithStart(name), getStopRequestWithCallback(name), getNotificationsRequest());
        }
        return createMockTask(name, mode, getRequestWithStart(name), getStopRequestWithCallback(name), null);
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

    public static CreateGraphRequest getRequestFromSingleTask(CreateTaskDTO task) {
        return CreateGraphRequest.builder()
                .vertex(task.name, task)
                .build();
    }

    public static CreateTaskDTO createMockTask(String name, Mode mode, org.jboss.pnc.api.dto.Request startRequest, org.jboss.pnc.api.dto.Request stopRequest, org.jboss.pnc.api.dto.Request notificationsRequest) {
        return createMockTask(name, mode, startRequest, stopRequest, notificationsRequest, null);
    }

    public static CreateTaskDTO createMockTask(String name, Mode mode, org.jboss.pnc.api.dto.Request startRequest, org.jboss.pnc.api.dto.Request stopRequest, org.jboss.pnc.api.dto.Request notificationsRequest, ConfigurationDTO config) {
        return CreateTaskDTO.builder()
                .name(name)
                .controllerMode(mode)
                .remoteStart(startRequest)
                .remoteCancel(stopRequest)
                .callerNotifications(notificationsRequest)
                .configuration(config)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getRequestWithoutStart(Object payload) {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/accept"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getStopRequest(Object payload) {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/stop"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getStopRequestWithCallback(Object payload) {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/stopAndCallback"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getRequestWithStart(Object payload) {
        return getRequestWithStart(payload, List.of());
    }
    public static org.jboss.pnc.api.dto.Request getRequestWithStart(Object payload, List<org.jboss.pnc.api.dto.Request.Header> headers) {
        var headerList = new ArrayList<>(headers);
        headerList.add(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json"));

        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/acceptAndStart"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(headerList)
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getRequestWithBackoff(Object payload, int failsUntilOK) {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/425eventuallyOK?fails=" + failsUntilOK))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getRequestWithNegativeCallback(Object payload) {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/test/acceptAndFail"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment(payload)
                .build();
    }

    public static org.jboss.pnc.api.dto.Request getNotificationsRequest() {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/transition/record"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment("hello")
                .build();
    }
    public static org.jboss.pnc.api.dto.Request getNaughtyNotificationsRequest() {
        return org.jboss.pnc.api.dto.Request.builder()
                .uri(URI.create("http://localhost:8081/transition/fail"))
                .method(org.jboss.pnc.api.dto.Request.Method.POST)
                .headers(List.of(new org.jboss.pnc.api.dto.Request.Header("Content-Type", "application/json")))
                .attachment("hello")
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
