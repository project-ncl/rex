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
package org.jboss.pnc.rex.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.exceptions.CircularDependencyException;
import org.jboss.pnc.rex.dto.responses.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Slf4j
@Provider
public class CircularDependencyExceptionMapper implements ExceptionMapper<CircularDependencyException> {
    @Override
    public Response toResponse(CircularDependencyException e) {
        Response.Status status = Response.Status.BAD_REQUEST;
        log.warn("Scheduling request results in circle: " + e, e);
        return Response.status(status)
                .entity(new ErrorResponse(e))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
