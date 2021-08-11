package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.CallerNotificationClient;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;

import static javax.enterprise.event.TransactionPhase.AFTER_SUCCESS;

public class NotifyCallerJob extends ControllerJob {

    private static final Logger log = LoggerFactory.getLogger(NotifyCallerJob.class);

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.AFTER_SUCCESS;

    private final Transition transition;

    private final CallerNotificationClient client;

    public NotifyCallerJob(Transition transition, Task task) {
        super(INVOCATION_PHASE, task);
        this.transition = transition;
        this.client = CDI.current().select(CallerNotificationClient.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        client.notifyCaller(transition, context);
        return true;
    }

    @Override
    void onException(Throwable e) {
        log.error("NOTIFICATION " + context.getName() + ": UNEXPECTED exception has been thrown.", e);
    }
}
