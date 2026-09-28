/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.delegates;

import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.rex.core.api.RollbackManager;

@WithRetries
@ApplicationScoped
public class TolerantRollbackManager implements RollbackManager {

    private final RollbackManager delegate;

    public TolerantRollbackManager(RollbackManager delegate) {
        this.delegate = delegate;
    }

    @Override
    @ApplyGuard("internal-retry")
    public void rollbackFromMilestone(String name) {
        delegate.rollbackFromMilestone(name);
    }
}