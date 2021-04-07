package org.jboss.pnc.scheduler.core.api;

import org.jboss.pnc.scheduler.core.model.TaskGraph;
import org.jboss.pnc.scheduler.model.Task;

import java.util.Set;

/**
 * Target where Tasks are installed into and removed from.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskTarget {

    /**
     * Removes a task from the Target. It has to be in the {@code ServiceController.StateGroup.FINAL}
     *
     * @param task the unique task name
     */
    void removeTask(String task);

    Set<Task> install(TaskGraph serviceBuilder);
}
