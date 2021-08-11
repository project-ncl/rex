package org.jboss.pnc.scheduler.core.jobs;

import io.smallrye.mutiny.Uni;
import org.jboss.pnc.scheduler.core.RemoteEntityClient;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.core.delegates.WithTransactions;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

public class InvokeStartJob extends ControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final RemoteEntityClient client;

    private final TaskController controller;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStartJob.class);

    public InvokeStartJob(Task task) {
        super(INVOCATION_PHASE, task);
        this.client = CDI.current().select(RemoteEntityClient.class).get();
        this.controller = CDI.current().select(TaskController.class, () -> WithTransactions.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        logger.info("START {}: STARTING", context.getName());
        client.startJob(context);
        return true;
    }

    @Override
    void onException(Throwable e) {
        logger.error("STOP " + context.getName() + ": UNEXPECTED exception has been thrown.", e);
        Uni.createFrom().voidItem()
                .onItem().invoke((ignore) -> controller.fail(context.getName(), "START : System failure. Exception: " + e.toString()))
                .onFailure().invoke((throwable) -> logger.warn("START " + context.getName() + ": Failed to transition task to START_FAILED state. Retrying.", throwable))
                .onFailure().retry().atMost(5)
                .onFailure().recoverWithNull()
                .await().indefinitely();
    }

}