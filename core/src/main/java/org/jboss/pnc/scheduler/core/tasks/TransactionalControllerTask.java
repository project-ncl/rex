package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.ServiceControllerImpl;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

public abstract class TransactionalControllerTask extends ControllerTask {

    protected final TransactionManager tm;

    TransactionalControllerTask(ServiceControllerImpl controller, TransactionManager tm) {
        super(controller);
        this.tm = tm;
    }

    @Override
    void beforeExecute() {
        if (notInTransaction()) {
            try {
                tm.begin();
            } catch (NotSupportedException | SystemException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown", e);
            }
        }
    }

    @Override
    void afterExecute() {
        if (notInTransaction()) {
            try {
                tm.commit();
            } catch (RollbackException e) {
                throw new IllegalStateException("Transaction rolled back!", e);
            } catch (SystemException | HeuristicRollbackException | HeuristicMixedException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown while committing transactions", e);
            }
        }
    }

    private boolean notInTransaction() {
        try {
            if (tm.getTransaction() == null) {
                return true;
            }
        } catch (SystemException e) {
            return true;
        }
        return false;
    }
}
