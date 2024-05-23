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
package org.jboss.pnc.rex.facade;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.pnc.rex.core.FailoverInitiator;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.facade.api.MaintenanceProvider;


@ApplicationScoped
public class MaintenanceProviderImpl implements MaintenanceProvider {

    private final TaskRegistry taskRegistry;

    private final QueueManager queueManager;

    private final ClusteredJobRegistry jobRegistry;

    private final FailoverInitiator failoverInitiator;

    public MaintenanceProviderImpl(TaskRegistry taskRegistry,
                                   QueueManager queueManager,
                                   ClusteredJobRegistry jobRegistry,
                                   FailoverInitiator failoverInitiator) {
        this.taskRegistry = taskRegistry;
        this.queueManager = queueManager;
        this.jobRegistry = jobRegistry;
        this.failoverInitiator = failoverInitiator;
    }

    @Override
    @Transactional
    public void clearEverything() {
        // remove all tasks
        taskRegistry.removeAllTasks();

        // synchronize counter
        queueManager.synchronizeRunningCounter();

        // clear job registry
        jobRegistry.deleteAll();

        // clears signal cache
        failoverInitiator.clearCaches();
    }
}
