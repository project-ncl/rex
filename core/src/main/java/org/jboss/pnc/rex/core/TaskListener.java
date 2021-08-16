package org.jboss.pnc.rex.core;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.core.jobs.ControllerJob;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@ApplicationScoped
@Slf4j
public class TaskListener {

    @Inject
    TransactionManager tm;

    void onUnsuccessfulTransaction(@Observes(during = TransactionPhase.AFTER_SUCCESS) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.AFTER_SUCCESS) {
            // disassociate the thread from previous transaction as it results in errors
            try {
                tm.suspend();
            } catch (SystemException e) {
                log.error("Could not disassociate from transaction.", e);
            }
            String contextMessage = task.getContext().isPresent() ? ' ' + task.getContext().get().getName() : "";
            log.debug("AFTER TRANSACTION{}: {}", contextMessage, task.getClass().getSimpleName());
            task.run();
        }
    }

    void onOngoingTransaction(@Observes(during = TransactionPhase.IN_PROGRESS) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.IN_PROGRESS) {
            String contextMessage = task.getContext().isPresent() ? ' ' + task.getContext().get().getName() : "";
            log.debug("WITHIN TRANSACTION{}: {}", contextMessage, task.getClass().getSimpleName());
            task.run();
        }
    }
}
