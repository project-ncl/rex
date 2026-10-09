/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.tolerance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jboss.pnc.rex.core.config.InternalRetryPolicy;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;
import lombok.extern.slf4j.Slf4j;

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
