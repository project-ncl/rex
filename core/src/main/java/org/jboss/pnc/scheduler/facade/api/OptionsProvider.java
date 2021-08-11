package org.jboss.pnc.scheduler.facade.api;

import org.jboss.pnc.scheduler.dto.responses.LongResponse;

/**
 * Public interface for managing scheduler's settings on runtime.
 */
public interface OptionsProvider {

    /**
     * Sets the maximum amount of concurrent Tasks. The method does not have effect on already running Tasks.
     *
     * @param amount amount to be set
     */
    void setConcurrency(Long amount);

    /**
     * Return current amount of concurrent builds.
     *
     * @return maximum concurrent Tasks
     */
    LongResponse getConcurrency();
}
