/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest.providers;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.security.spi.runtime.AuthorizationController;
import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;

/**
 * This class provides an option to disable authorization for testing purposes.
 *
 * @see <a href="https://quarkus.io/guides/security-customization#disabling-authorization">Quarkus Security Guide</a>
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
// tests have their own mechanism for disabling authorization
@UnlessBuildProfile("test")
public class DisabledAuthController extends AuthorizationController {
    @ConfigProperty(name = "disable.authorization", defaultValue = "false")
    boolean disableAuthorization;

    @Override
    public boolean isAuthorizationEnabled() {
        return !disableAuthorization;
    }
}
