package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;

/**
 * This is API for ServiceController.
 * <p>
 * ServiceController is an main entity that manipulates and handles transitions for each scheduled remote job(Service).
 *
 * <p>
 * ServiceController does not hold any data besides ServiceName key of a Service that it is associated with. Before each method
 * is invoked it loads updated data from Container.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceController {
    /**
     * Gets unique name of an associated job(Service).
     *
     * @return the name
     */
    ServiceName getName();

    /**
     * Gets the Container in which the job(Service) is installed in.
     *
     * @return the container
     */
    ServiceContainer getContainer();

    /**
     * Gets mode of a Service.
     *
     * @return the mode
     */
    Mode getMode();

    /**
     * Sets mode of a Service. Needs to be called under a lock.
     *
     * @param mode the mode
     */
    void setMode(Mode mode);

    /**
     * Gets current Service state.
     *
     * @return the state
     */
    State getState();

    /**
     * Method used for positive callback. Needs to be called under a lock.
     *
     * f.e. to signalize that remote job(Service) has started/cancelled/finished.
     */
    void accept();

    /**
     * Method used for negative callback. Needs to be called under a lock.
     *
     * f.e. to signalize that remote job(Service) failed to start/cancel or failed during execution.
     */
    void fail();

}