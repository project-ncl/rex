/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.delegates;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.pnc.rex.core.api.CleaningManager;

import io.quarkus.arc.Unremovable;
import io.smallrye.faulttolerance.api.ApplyGuard;

@WithRetries
@Unremovable
@ApplicationScoped
public class TolerantCleaningManager implements CleaningManager {

    private final CleaningManager delegate;

    public TolerantCleaningManager(CleaningManager manager) {
        this.delegate = manager;
    }

    @Override
    @ApplyGuard("internal-retry")
    public void tryClean() {
        delegate.tryClean();
    }
}
