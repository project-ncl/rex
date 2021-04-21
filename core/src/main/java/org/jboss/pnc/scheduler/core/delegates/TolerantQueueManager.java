package org.jboss.pnc.scheduler.core.delegates;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.scheduler.core.api.QueueManager;

import javax.enterprise.context.ApplicationScoped;

@WithRetries
@ApplicationScoped
public class TolerantQueueManager implements QueueManager {

    private final QueueManager delegate;

    public TolerantQueueManager(QueueManager manager) {
        this.delegate = manager;
    }

    @Override
    @Retry
    public void poke() {
        delegate.poke();
    }

    @Override
    @Retry
    public void decreaseRunningCounter() {
        delegate.decreaseRunningCounter();
    }

    @Override
    @Retry
    public void setMaximumConcurrency(Long amount) {
        delegate.setMaximumConcurrency(amount);
    }

    @Override
    public Long getMaximumConcurrency() {
        return delegate.getMaximumConcurrency();
    }
}
