package org.jboss.pnc.scheduler.core.api;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;

/**
 * This is API for TaskController.
 * <p>
 * TaskController is the entity that handles transitions and schedules internal Jobs for each Task. It is a public API
 * for interacting with Tasks.
 *
 * <p>
 * TaskController does not hold any data.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskController {
    /**
     * Sets mode of a Task. Needs to be called in a transaction.
     *
     * @param name id of the Task
     * @param mode the mode
     */
    void setMode(String name, Mode mode);

    /**
     * Sets mode of a Task. Needs to be called in a transaction. Additionally pokes queue
     * after transaction succeeds if specified.
     *
     * @param name id of the Task
     * @param mode the mode
     * @param pokeQueue should pokeQueue
     */
    void setMode(String name, Mode mode, boolean pokeQueue);

    /**
     * Method used for positive callback. Needs to be called in a transaction.
     *
     * f.e. to signalize that remote Task has started/cancelled/finished.
     * @param name
     */
    void accept(String name, Object response);

    /**
     * Method used for negative callback. Needs to be called in a transaction.
     *
     * f.e. to signalize that remote Task failed to start/cancel or failed during execution.
     * @param name
     */
    void fail(String name, Object response);

    void dequeue(String name);
}