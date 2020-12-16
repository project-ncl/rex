package org.jboss.pnc.scheduler.core;

import org.jboss.pnc.scheduler.core.tasks.ControllerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@ApplicationScoped
public class TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskListener.class);

    @Inject
    TransactionManager tm;

    void onUnsuccessfulTransaction(@Observes(during = TransactionPhase.AFTER_SUCCESS) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.AFTER_SUCCESS) {
            // disassociate the thread from previous transaction as it results in errors
            try {
                tm.suspend();
            } catch (SystemException e) {
                e.printStackTrace();
            }
            logger.info("Running a " + task + " AFTER_SUCCESS");
            task.run();
        }
    }

    void onOngoingTransaction(@Observes(during = TransactionPhase.IN_PROGRESS) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.IN_PROGRESS) {
            logger.info("Running a " + task + " with IN_PROGRESS");
            task.run();
        }
    }
}
