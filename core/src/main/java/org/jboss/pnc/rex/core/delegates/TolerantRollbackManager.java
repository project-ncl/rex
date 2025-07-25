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