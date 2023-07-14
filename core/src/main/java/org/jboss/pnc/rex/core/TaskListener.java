/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.rex.core.jobs.ControllerJob;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

@ApplicationScoped
@Slf4j
public class TaskListener {

    @Inject
    TransactionManager tm;

    @Inject
    ManagedExecutor executor;

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
            if (task.isAsync()) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    void onOngoingTransaction(@Observes(during = TransactionPhase.IN_PROGRESS) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.IN_PROGRESS) {
            String contextMessage = task.getContext().isPresent() ? ' ' + task.getContext().get().getName() : "";
            log.debug("WITHIN TRANSACTION{}: {}", contextMessage, task.getClass().getSimpleName());
            if (task.isAsync()) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    void beforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) ControllerJob task) {
        if (task.getInvocationPhase() == TransactionPhase.BEFORE_COMPLETION) {
            String contextMessage = task.getContext().isPresent() ? ' ' + task.getContext().get().getName() : "";
            log.debug("BEFORE COMPLETION: " + contextMessage);
            if (task.isAsync()) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    void failureListener(
            @Observes(
                    during = TransactionPhase.AFTER_FAILURE) @BeforeDestroyed(TransactionScoped.class) @Priority(APPLICATION
                            + 499) Object ignore)
            throws SystemException {
        log.error("AFTER FAILURE: Transaction failed " + tm.getTransaction().toString());
    }

    void successListener(
            @Observes(
                    during = TransactionPhase.AFTER_SUCCESS) @BeforeDestroyed(TransactionScoped.class) @Priority(APPLICATION
                            + 499) Object ignore)
            throws SystemException {
        log.trace("AFTER SUCCESS: Transaction successful " + tm.getTransaction().toString());
    }
}
