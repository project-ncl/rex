package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;
import java.util.Optional;

/**
 * Template for creating Controller Jobs. Usually a ControllerJob is associated with a specific Task which triggered
 * said Job.
 *
 * Each Controller Job has to have a TransactionPhase defined. TransactionPhase will determine in which
 * transaction phase the Job is run (f.e. a Job should be executed only after a successful Transaction or a Job has to
 * be executed within the bounds of the Transaction which scheduled the Job). Jobs that run after Transactions have to
 * handle fault tolerance (Retries) and creating new transactions on their own.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
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
