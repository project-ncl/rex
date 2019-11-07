package org.jboss.pnc.scheduler.core.tasks;

public abstract class ControllerTask implements Runnable {

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
}
