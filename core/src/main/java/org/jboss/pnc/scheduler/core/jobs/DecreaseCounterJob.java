package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.api.QueueManager;
import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class DecreaseCounterJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    private final QueueManager queueManager;

    public DecreaseCounterJob(Task context) {
        super(INVOCATION_PHASE, context);
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
