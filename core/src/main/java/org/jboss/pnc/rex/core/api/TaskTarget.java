package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.model.Task;

import java.util.Set;

/**
 * Target where Tasks are installed into and removed from.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskTarget {

    /**
     * Removes a task from the Target. It has to be in the {@link FINAL} group state.
     *
     * @param task the unique task name
     */
    void removeTask(String task);

    /**
     * Starts scheduling a graph of Tasks. Vertices have to be NEW tasks. Edges can be between EXISTING or NEW tasks.
     * If an edge would introduce dependency relationship where the dependant is an EXISTING Task in {@link FINAL} or
     * {@link RUNNING} state, it will get rejected.
     *
     * @param taskGraph graph of task consisting of edges and vertices
     * @return new scheduled tasks
     */
    Set<Task> install(TaskGraph taskGraph);
}
