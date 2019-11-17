package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public abstract class SynchronizedAsyncControllerTask extends AsyncControllerTask {

    protected TransactionManager tm;

    public SynchronizedAsyncControllerTask(TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    void beforeExecute() {
        try {
            waitForTransactionToEnd(tm.getTransaction());
            if (isUnsuccessfulTransaction(tm.getTransaction())) {
                throw new ConcurrentUpdateException("Propagated transaction failed, aborting request");
            }
            tm.suspend();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void afterExecute() {

    }

    @Override
    void onException(Throwable e) {
        //TODO handle RetryException
    }

    private static void waitForTransactionToEnd(Transaction transaction) {
        while (true) {
            try {
                if (!transactionIsFinished(transaction)) {
                    return;
                }
                Thread.sleep(25);
            } catch (InterruptedException e) {
                //nope
            }
        }
    }

    private static boolean isUnsuccessfulTransaction(Transaction transaction) throws SystemException {
        if (transaction == null) {
            return false;
        }
        return transaction.getStatus() != Status.STATUS_COMMITTED;
    }

    private static boolean transactionIsFinished(Transaction transaction){
        if (transaction == null) {
            return false;
        }
        try {
            switch (transaction.getStatus()) {
                case Status.STATUS_COMMITTING:
                case Status.STATUS_PREPARING:
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_ACTIVE:
                    return true;
                case Status.STATUS_COMMITTED:
                case Status.STATUS_ROLLEDBACK:
                case Status.STATUS_UNKNOWN:
                case Status.STATUS_NO_TRANSACTION:
                    return false;
                default:
                    throw new IllegalStateException("Unexpected value: " + transaction.getStatus());
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Transaction failed in an unexpected way",e);
        }
    }
}
