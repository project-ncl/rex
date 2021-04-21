package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.api.QueueManager;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class DecreaseCounterJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final QueueManager queueManager;

    public DecreaseCounterJob() {
        super(INVOCATION_PHASE);
        this.queueManager = CDI.current().select(QueueManager.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        queueManager.decreaseRunningCounter();
        return true;
    }

    @Override
    void onException(Throwable e) {}
}
