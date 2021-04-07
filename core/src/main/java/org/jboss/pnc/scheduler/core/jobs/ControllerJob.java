package org.jboss.pnc.scheduler.core.jobs;

import javax.enterprise.event.TransactionPhase;

public abstract class ControllerJob implements Runnable {

    protected TransactionPhase invocationPhase;

    protected ControllerJob(TransactionPhase invocationPhase) {
        this.invocationPhase = invocationPhase;
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
}
