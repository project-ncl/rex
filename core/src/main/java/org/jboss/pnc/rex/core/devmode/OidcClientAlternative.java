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
package org.jboss.pnc.rex.core.devmode;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Duration;
import java.util.Map;

@ApplicationScoped
@LookupIfProperty(name = "quarkus.oidc-client.enabled", stringValue = "false")
@IfBuildProfile(anyOf = {"dev", "test", "local"})
/*
 * To be able to start in development/test mode without authorization
 */
public class OidcClientAlternative {
    @Produces
    public OidcClient produceToken() {
        return new OidcAlt();
    }

    private static class OidcAlt implements OidcClient {
        @Override
        public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
            return Uni.createFrom().item(new Tokens("access-token",
                Long.MAX_VALUE,
                Duration.ofNanos(Long.MAX_VALUE),
                "refresh-token",
                Long.MAX_VALUE,
                JsonObject.of(),
                ""));
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
            return getTokens();
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
            return Uni.createFrom().item(true);
        }

        @Override
        public void close() {}
    }
}
