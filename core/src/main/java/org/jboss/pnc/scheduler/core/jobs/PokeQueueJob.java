package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.api.QueueManager;
import org.jboss.pnc.scheduler.core.delegates.WithRetries;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class PokeQueueJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final QueueManager manager;

    public PokeQueueJob() {
        super(INVOCATION_PHASE);
        this.manager = CDI.current().select(QueueManager.class, () -> WithRetries.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        manager.poke();
        return true;
    }

    @Override
    void onException(Throwable e) {}
}
