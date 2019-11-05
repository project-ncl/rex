package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.ServiceControllerImpl;

public abstract class ControllerTask implements Runnable {
    protected final ServiceControllerImpl controller;

    ControllerTask(ServiceControllerImpl controller) {
        this.controller = controller;
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

    void onException(Throwable e) {};
    abstract void beforeExecute();
    abstract void afterExecute();
    abstract boolean execute();
}
