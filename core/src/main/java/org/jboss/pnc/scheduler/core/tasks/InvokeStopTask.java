package org.jboss.pnc.scheduler.core.tasks;

import javax.transaction.TransactionManager;

public class InvokeStopTask extends TransactionalControllerTask{
    public InvokeStopTask(TransactionManager tm) {
        super(tm);
    }

    @Override
    boolean execute() {
        return false;
    }
}
