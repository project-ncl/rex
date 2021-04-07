package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.RemoteEntityClient;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class InvokeStopJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final Task task;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStopJob.class);

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    void onException(Throwable e) {}

    public InvokeStopJob(Task task) {
        super(INVOCATION_PHASE);
        this.task = task;
        this.client = CDI.current().select(RemoteEntityClient.class).get();
    }

    @Override
    boolean execute() {
        logger.info("Invoking StopJob for " + task.getName());
        client.stopJob(task);
        return true;
    }
}
