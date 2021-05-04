package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.CallerNotificationClient;
import org.jboss.pnc.scheduler.model.Task;


import javax.enterprise.inject.spi.CDI;

import static javax.enterprise.event.TransactionPhase.AFTER_SUCCESS;

public class NotifyCallerJob extends ControllerJob {

    private final Transition transition;

    private final Task task;

    private final CallerNotificationClient client;

    public NotifyCallerJob(Transition transition, Task task) {
        super(AFTER_SUCCESS);
        this.transition = transition;
        this.task = task;
        this.client = CDI.current().select(CallerNotificationClient.class).get();
    }

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    boolean execute() {
        client.notifyCaller(transition, task);
        return true;
    }

    @Override
    void onException(Throwable e) {}
}
