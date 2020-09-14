package org.jboss.pnc.scheduler.core.api;

/**
 * Target where Tasks are installed into and removed from.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskTarget {
    /**
     * Method serves as a way to create new Tasks to install into TaskTarget.
     * <p>
     * Returns BatchTaskInstaller that is used to create Tasks that are installed in the Target
     * <p>
     *
     * To install a Controller and start its scheduling:
     *  1. use {@code BatchServiceInstaller.addTask()}.
     *  2. declare the Task through returned TaskBuilder and
     *  3. use TaskBuilder.install() to commit the changes to the batch
     *  4. use {@code BatchTaskInstaller.commit()} to commit the batch to the target start scheduling
     *
     * @param service the unique ServiceName for new Tasks
     * @return the task builder
     */
    BatchTaskInstaller addTasks();

    /**
     * Removes a task from the Target. It has to be in the {@code ServiceController.StateGroup.FINAL}
     *
     * @param task the unique task name
     */
    void removeTask(String task);
}
