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
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.core.api.CleaningManager;
import org.jboss.pnc.rex.core.delegates.WithRetries;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;

public class PokeCleanJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final CleaningManager manager;

    public PokeCleanJob() {
        super(INVOCATION_PHASE, null, false);
        this.manager = CDI.current().select(CleaningManager.class, () -> WithRetries.class).get();
    }

    @Override
    protected void beforeExecute() {

    }

    @Override
    protected void afterExecute() {

    }

    @Override
    public boolean execute() {
        manager.tryClean();
        return true;
    }

    @Override
    protected void onException(Throwable e) {}

    @Override
    protected void onFailure() {}
}
