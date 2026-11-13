/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.pnc.rex.facade.api.MaintenanceProvider;

import io.smallrye.faulttolerance.api.ApplyGuard;

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
