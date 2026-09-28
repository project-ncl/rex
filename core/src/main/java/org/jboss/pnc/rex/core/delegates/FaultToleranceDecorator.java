/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.delegates;

import io.quarkus.arc.Unremovable;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.function.Supplier;

@Unremovable
@ApplicationScoped
public class FaultToleranceDecorator {

    @ApplyGuard("internal-retry")
    public void withTolerance(Runnable runnable) {
        runnable.run();
    }

    @ApplyGuard("internal-retry")
    public <T> T withTolerance(Supplier<T> supplier) {
        return supplier.get();
    }
}
