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

import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.pnc.rex.facade.api.MaintenanceProvider;

@ApplicationScoped
public class MaintenanceEndpointImpl implements MaintenanceEndpoint {

    private final MaintenanceProvider provider;

    public MaintenanceEndpointImpl(MaintenanceProvider provider) {
        this.provider = provider;
    }

    @Override
    @RolesAllowed({ "pnc-app-rex-editor", "pnc-users-admin" })
    @ApplyGuard("internal-retry")
    public void clearAll() {
        provider.clearEverything();
    }
}
