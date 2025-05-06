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

import org.jboss.pnc.rex.core.api.DependentMessenger;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Set;

/**
 * Jobs implementing this abstract class are used for messaging all dependants about important changes(dependency has
 * finished, failed...)
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public abstract class DependantMessageJob extends ControllerJob {

    private final Set<String> dependents;

    protected DependentMessenger dependentAPI;

    protected DependantMessageJob(Task task, TransactionPhase invocationPhase) {
        super(invocationPhase, task, false);
        this.dependents = task.getDependants();
        this.dependentAPI = CDI.current().select(DependentMessenger.class).get();
    }

    @Override
    public boolean execute() {
        for (String dependent : dependents) {
            inform(dependent);
        }
        return true;
    }

    protected abstract void inform(final String dependentName);

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}
}
