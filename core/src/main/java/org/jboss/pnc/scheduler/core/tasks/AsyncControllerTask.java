package org.jboss.pnc.scheduler.core.tasks;

import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.inject.spi.CDI;

public abstract class AsyncControllerTask extends ControllerTask {

    ManagedExecutor managedExecutor;

    public AsyncControllerTask() {
        this.managedExecutor = CDI.current().select(ManagedExecutor.class).get();;
    }

    @Override
    public void run() {
        managedExecutor.submit(super::run);
    }

}
