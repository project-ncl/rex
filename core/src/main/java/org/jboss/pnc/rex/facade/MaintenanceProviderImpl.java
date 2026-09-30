/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
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
