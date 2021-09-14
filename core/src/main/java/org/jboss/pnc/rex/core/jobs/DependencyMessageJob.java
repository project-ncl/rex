/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

import org.jboss.pnc.rex.core.api.DependencyMessenger;
import org.jboss.pnc.rex.model.Task;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;
import java.util.Set;

public abstract class DependencyMessageJob extends ControllerJob {

    private final Set<String> dependencies;

    protected DependencyMessenger dependencyAPI;

    protected DependencyMessageJob(Task task, TransactionPhase invocationPhase) {
        super(invocationPhase, task, false);
        this.dependencies = task.getDependencies();
        this.dependencyAPI = CDI.current().select(DependencyMessenger.class).get();
    }

    @Override
    boolean execute() {
        for (String dependent : dependencies) {
            inform(dependent);
        }
        return true;
    }

    abstract void inform(final String dependencyName);

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    void onException(Throwable e) {}
}
