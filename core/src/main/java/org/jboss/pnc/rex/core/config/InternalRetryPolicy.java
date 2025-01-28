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
package org.jboss.pnc.rex.core.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.faulttolerance.api.FaultTolerance;
import org.jboss.pnc.rex.core.config.api.MPRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConfigMapping(prefix = "scheduler.options.internal-retry-policy") //CDI
public interface InternalRetryPolicy extends MPRetryPolicy {
    Logger log = LoggerFactory.getLogger(InternalRetryPolicy.class);

    String DESCRIPTION =
            """
            Fault Tolerance Policy (mainly Retries) that maintains consistency
            of data in Infinispan. In case a transaction fails (concurrent modification), it is
            automatically retried from the earliest point where data was accessed.
            Newly spawned transaction will do the same operations but with up-to-date
            data. Therefore the operation/action is not lost and data should be consistent.
            """;

    default FaultTolerance.Builder<Object, FaultTolerance<Object>> toleranceBuilder() {
        return MPRetryPolicy.super.toleranceBuilder(
                Object.class,
                () -> log.debug("Transaction may failed. This is normal. Retrying actions!"),
                DESCRIPTION
        );
    }
}
