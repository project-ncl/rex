package org.jboss.pnc.scheduler.core.tasks;

import javax.transaction.TransactionManager;

public class InvokeStartTask extends TransactionalControllerTask{
    public InvokeStartTask(TransactionManager tm) {
        super(tm);
    }

    @Override
    boolean execute() {
        return false;
    }
}
