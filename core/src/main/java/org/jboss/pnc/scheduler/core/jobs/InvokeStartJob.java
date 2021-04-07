package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.RemoteEntityClient;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class InvokeStartJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final Task task;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStartJob.class);

    public InvokeStartJob(Task task) {
        super(INVOCATION_PHASE);
        this.task = task;
        this.client = CDI.current().select(RemoteEntityClient.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        logger.info("Invoking StartJob for " + task.getName());
        client.startJob(task);
        return true;
    }

    @Override
    void onException(Throwable e) {}

}