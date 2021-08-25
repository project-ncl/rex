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
package org.jboss.pnc.rex.test.profile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

public class WithWiremockOpenId implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.oidc.enabled", "true",
                "quarkus.oidc.auth-server-url","${keycloak.url}/realms/quarkus/",
                "quarkus.oidc.client-id", "quarkus-service-app",
                "quarkus.oidc.application-type","service",
                "quarkus.test.oidc.token.admin-roles", "system-user",
                "smallrye.jwt.sign.key.location","privateKey.jwk");
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