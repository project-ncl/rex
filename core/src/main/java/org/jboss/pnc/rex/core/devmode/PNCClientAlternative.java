/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.devmode;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jboss.pnc.quarkus.client.auth.runtime.PNCClientAuth;

import java.io.IOException;

@ApplicationScoped
@LookupIfProperty(name = "quarkus.oidc-client.enabled", stringValue = "false")
@IfBuildProfile(anyOf = {"dev", "test", "local"})
/*
 * To be able to start in development/test mode without authorization
 */
public class PNCClientAlternative {
    @Produces
    public PNCClientAuth produceToken() {
        return new PNCClientAuthAlt();
    }

    private static class PNCClientAuthAlt implements PNCClientAuth {

        @Override
        public ClientAuthType getConfiguredType() {
            return ClientAuthType.OIDC;
        }

        @Override
        public String getAuthToken() {
            return "1234";
        }

        @Override
        public String getHttpAuthorizationHeaderValue() {
            return "Bearer 1234";
        }

        @Override
        public String getHttpAuthorizationHeaderValueWithCachedToken() {
            return getHttpAuthorizationHeaderValue();
        }

        @Override
        public LDAPCredentials getLDAPCredentials() throws IOException {
            return new LDAPCredentials("user", "password");
        }
    }
}
