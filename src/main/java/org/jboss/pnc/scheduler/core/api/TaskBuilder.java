package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.model.RemoteAPI;

/**
 * ServiceBuilders are used for creating jobs(Services) and their installment into the container
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface TaskBuilder {
    /**
     * Registers a dependency(child)
     *
     * @param dependency the task
     * @return the task builder
     */
    TaskBuilder requires(ServiceName dependency);

    /**
     * Registers a dependent(parent)
     *
     * @param dependant the task
     * @return the task builder
     */
    TaskBuilder isRequiredBy(ServiceName dependant);

    /**
     * Set initial mode
     *
     * @param mode initialMode (default is Mode.IDLE)
     * @return the task builder
     */
    TaskBuilder setInitialMode(Mode mode);

    /**
     * Set remote communication
     *
     * @param api endpoints invoking start and stop urls
     * @return the task builder
     */
    TaskBuilder setRemoteEndpoints(RemoteAPI api);

    /**
     * Set payload that is sent to remote entity
     *
     * @param payload generic payload
     * @return the task builder
     */
    TaskBuilder setPayload(String payload);

    /**
     * Commit changes to the BatchTaskBuilder
     *
     */
    void install();
}
