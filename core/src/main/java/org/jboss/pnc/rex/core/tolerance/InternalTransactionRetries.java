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
package org.jboss.pnc.rex.core.tolerance;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.core.config.InternalRetryPolicy;


@Slf4j
@ApplicationScoped
public class InternalTransactionRetries {

    @Produces
    @Singleton
    @Identifier("internal-retry")
    public static Guard internalRetry(InternalRetryPolicy internalRetryPolicy) {
        return internalRetryPolicy.toleranceBuilder().build();
    }
}
