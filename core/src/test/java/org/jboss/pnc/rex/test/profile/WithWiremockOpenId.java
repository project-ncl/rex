/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.profile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

public class WithWiremockOpenId implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.oidc.enabled",
                "true",
                "quarkus.oidc.auth-server-url",
                "${keycloak.url}/realms/quarkus/",
                "quarkus.oidc.client-id",
                "quarkus-service-app",
                "quarkus.oidc.application-type",
                "service",
                "quarkus.test.oidc.token.admin-roles",
                "system-user",
                "smallrye.jwt.sign.key.location",
                "privateKey.jwk");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(OidcWiremockTestResource.class, Collections.emptyMap(), true));
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }
}