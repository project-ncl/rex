package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;
import java.util.Optional;

public abstract class ControllerJob implements Runnable {

    protected TransactionPhase invocationPhase;

    protected Task context;

    protected ControllerJob(TransactionPhase invocationPhase, Task context) {
        this.invocationPhase = invocationPhase;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            beforeExecute();
            if (!execute()) return;
        } catch (RuntimeException e) {
            onException(e);
            throw e;
        } finally {
            afterExecute();
        }
    }

    abstract void beforeExecute();
    abstract void afterExecute();
    abstract boolean execute();
    abstract void onException(Throwable e);
    public TransactionPhase getInvocationPhase() {
        return invocationPhase;
    }
    public Optional<Task> getContext() {
        return context == null ? Optional.empty() : Optional.of(context);
    }
}
