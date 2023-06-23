/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.rex.api.VersionEndpoint;
import org.jboss.pnc.rex.common.Constants;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;

@Tag(name = "Version Endpoint")
@Path("/rest/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VersionEndpointImpl implements VersionEndpoint {

    @ConfigProperty(name = "quarkus.application.name")
    String name;

    @Override
    public ComponentVersion getVersion() {
        return ComponentVersion.builder()
                .name(name)
                .version(Constants.REX_VERSION)
                .commit(Constants.COMMIT_HASH)
                .builtOn(ZonedDateTime.parse(Constants.BUILD_TIME))
                .build();
    }
}
