package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;

/**
 * This is API for TaskController.
 * <p>
 * TaskController is an main entity that manipulates and handles transitions for each scheduled remote Task.
 *
 * <p>
 * TaskController does not hold any data besides ServiceName key of a Task that it is associated with. Before each method
 * is invoked it loads updated data from Container.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskController {
    /**
     * Gets unique name of an associated Task.
     *
     * @return the name
     */
    ServiceName getName();

    /**
     * Gets the Container in which the Task is installed in.
     *
     * @return the container
     */
    TaskContainer getContainer();

    /**
     * Gets mode of a Task.
     *
     * @return the mode
     */
    Mode getMode();

    /**
     * Sets mode of a Task. Needs to be called under a lock.
     *
     * @param mode the mode
     */
    void setMode(Mode mode);

    /**
     * Gets current Task state.
     *
     * @return the state
     */
    State getState();

    /**
     * Method used for positive callback. Needs to be called in a transaction.
     *
     * f.e. to signalize that remote Task has started/cancelled/finished.
     */
    void accept();

    /**
     * Method used for negative callback. Needs to be called in a transaction.
     *
     * f.e. to signalize that remote Task failed to start/cancel or failed during execution.
     */
    void fail();

}