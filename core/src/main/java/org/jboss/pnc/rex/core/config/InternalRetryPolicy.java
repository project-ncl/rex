/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.config;

import org.jboss.pnc.rex.core.config.api.MPRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.ConfigMapping;
import io.smallrye.faulttolerance.api.Guard;

@ConfigMapping(prefix = "scheduler.options.internal-retry-policy") //CDI
public interface InternalRetryPolicy extends MPRetryPolicy {
    Logger log = LoggerFactory.getLogger(InternalRetryPolicy.class);

    String DESCRIPTION = """
            Fault Tolerance Policy (mainly Retries) that maintains consistency
            of data in Infinispan. In case a transaction fails (concurrent modification), it is
            automatically retried from the earliest point where data was accessed.
            Newly spawned transaction will do the same operations but with up-to-date
            data. Therefore the operation/action is not lost and data should be consistent.
            """;

    default Guard.Builder toleranceBuilder() {
        return MPRetryPolicy.super.toleranceBuilder(
                Object.class,
                () -> log.debug("Transaction may failed. This is normal. Retrying actions!"),
                DESCRIPTION);
    }
}
