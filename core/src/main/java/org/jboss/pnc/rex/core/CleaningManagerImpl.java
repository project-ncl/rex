/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.pnc.rex.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.core.api.CleaningManager;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.model.Task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class CleaningManagerImpl implements CleaningManager {

    private final TaskContainer container;
    private final TaskController controller;

    public CleaningManagerImpl(TaskContainer container, TaskController controller) {
        this.container = container;
        this.controller = controller;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void tryClean() {
        log.info("CLEANER: Querying for tasks ready for deletion.");

        List<Task> tasksToDelete = container.getMarkedTasksWithoutDependants();

        if (tasksToDelete.isEmpty()) {
            log.info("CLEANER: No immediately disposable tasks were found.");
            return;
        }

        log.info(
                "CLEANER: Found {} top-level tasks for deletion {}. The deletion can cascade to their dependencies.",
                tasksToDelete.size(),
                tasksToDelete.stream().map(Task::getName).collect(Collectors.toList()));
        try {
            tasksToDelete.forEach(task -> controller.delete(task.getName()));
        } catch (TaskMissingException e) {
            // race-condition when task gets removed before list query and execution delete() method
            // the transaction should be retried
            throw new ConcurrentUpdateException(
                    "Task " + e.getTaskName() + " was remotely updated during the transaction",
                    e);
        }

        log.info("CLEANER: Cleaning completed.");
    }
}
