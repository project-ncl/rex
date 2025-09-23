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
package org.jboss.pnc.rex.rest;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.pnc.rex.common.enums.Transition;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.core.jobs.NotifyCallerJob;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.facade.mapper.TaskMapper;

@Path("/rest/test")
public class TestEndpointImpl {

    @Inject
    TaskMapper taskMapper;

    @Inject
    Event<ControllerJob> jobEvent;


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void resendNotification(TaskDTO taskDTO) {

        NotifyCallerJob job = new NotifyCallerJob(Transition.UP_to_SUCCESSFUL, taskMapper.toDB(taskDTO));

        jobEvent.fire(job);

        return;
    }
}
