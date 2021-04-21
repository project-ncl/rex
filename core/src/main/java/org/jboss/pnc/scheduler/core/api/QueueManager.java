package org.jboss.pnc.scheduler.core.api;

public interface QueueManager {
    void poke();
    void decreaseRunningCounter();
    void setMaximumConcurrency(Long amount);
    Long getMaximumConcurrency();
}
