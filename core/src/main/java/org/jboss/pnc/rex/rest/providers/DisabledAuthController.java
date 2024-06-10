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
 * @see https://quarkus.io/guides/security-customization#disabling-authorization
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
