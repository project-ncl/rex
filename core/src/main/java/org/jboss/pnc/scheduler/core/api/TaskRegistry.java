
package org.jboss.pnc.scheduler.core.api;

import org.jboss.pnc.scheduler.common.exceptions.TaskMissingException;
import org.jboss.pnc.scheduler.model.Task;

import java.util.Collection;
import java.util.List;

/**
 * The registry is used to retrieve Tasks.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskRegistry {
    /**
     * Returns the Task for a unique ServiceName.
     *
     * @param task the serviceName of the service
     * @return the task or null if doesn't exist
     */
    Task getTask(String task);

    /**
     * Returns the Task for a unique ServiceName. Throws an exception if not found.
     *
     * @param task the task name
     * @return the task
     * @throws TaskMissingException the task was not found
     */
    Task getRequiredTask(String task) throws TaskMissingException;

    /**
     * Returns all Tasks present in the cache filtered by parameters
     *
     * (Can be costly without filters)
     *
     * @param waiting is in StateGroup.IDLE state
     * @param running is in StateGroup.RUNNING state
     * @param finished is in StateGroup.FINAL state
     * @return list of filtered services
     */
    List<Task> getTask(boolean waiting, boolean running, boolean finished);

    List<Task> getEnqueuedTasks(long limit);

    /**
     * Returns all task identifiers in clustered container.
     *
     * @return the service names
     */
    Collection<String> getTaskIds();
}
