/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core;

import io.quarkus.vertx.core.runtime.VertxMDC;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import jakarta.annotation.Priority;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.rex.core.jobs.ControllerJob;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;
import org.slf4j.MDC;

import java.util.concurrent.ConcurrentHashMap;

import static jakarta.interceptor.Interceptor.Priority.APPLICATION;

@ApplicationScoped
@Slf4j
public class TaskListener {

    private final TransactionManager tm;

    private final ManagedExecutor executor;

    public TaskListener(TransactionManager tm, ManagedExecutor executor) {
        this.tm = tm;
        this.executor = executor;
    }

    void onSuccessfulTransaction(@Observes(during = TransactionPhase.AFTER_SUCCESS) ControllerJob job) {
        if (job.getInvocationPhase() == TransactionPhase.AFTER_SUCCESS) {
            // disassociate the thread from previous transaction as it results in errors
            try {
                tm.suspend();
            } catch (SystemException e) {
                log.error("Could not disassociate from transaction.", e);
            }

            String contextMessage = job.getContext().isPresent() ? ' ' + job.getContext().get().getName() : "";
            log.debug("AFTER TRANSACTION{}: {}", contextMessage, job.getClass().getSimpleName());
            if (job.isAsync()) {
                executor.execute(() -> correctlyPropagateMDC(job));
            } else {
                job.run();
            }
        }
    }

    void onOngoingTransaction(@Observes(during = TransactionPhase.IN_PROGRESS) ControllerJob job) {
        if (job.getInvocationPhase() == TransactionPhase.IN_PROGRESS) {
            String contextMessage = job.getContext().isPresent() ? ' ' + job.getContext().get().getName() : "";
            log.debug("WITHIN TRANSACTION{}: {}", contextMessage, job.getClass().getSimpleName());
            if (job.isAsync()) {
                executor.execute(() -> correctlyPropagateMDC(job));
            } else {
                job.run();
            }
        }
    }

    void beforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) ControllerJob job) {
        if (job.getInvocationPhase() == TransactionPhase.BEFORE_COMPLETION) {
            String contextMessage = job.getContext().isPresent() ? ' ' + job.getContext().get().getName() : "";
            log.debug("BEFORE COMPLETION: {}", contextMessage);
            if (job.isAsync()) {
                executor.execute(() -> correctlyPropagateMDC(job));
            } else {
                job.run();
            }
        }
    }

    /**
     * Inspired by VertxMDC#contextualDataMap(Context ctx) to reset Map instance
     */
    void correctlyPropagateMDC(Runnable job) {
        var mdcCopy = MDC.getCopyOfContextMap();
        Context context = Vertx.currentContext();
        if (context != null) {
            var contextData = ((ContextInternal) context).localContextData();
            if (contextData.containsKey(VertxMDC.class.getName())) {
                // create a NEW instance of propagated MDC map to avoid parallel threads influencing each other
                contextData.put(VertxMDC.class.getName(), new ConcurrentHashMap<>(mdcCopy));
            }
        }

        job.run();
    }

    void failureListener(@Observes(during = TransactionPhase.AFTER_FAILURE) @BeforeDestroyed(TransactionScoped.class) @Priority(APPLICATION + 499) Object ignore) throws SystemException {
        log.warn("AFTER FAILURE: Transaction failed {}", tm.getTransaction().toString());
    }
    void successListener(@Observes(during = TransactionPhase.AFTER_SUCCESS) @BeforeDestroyed(TransactionScoped.class) @Priority(APPLICATION + 499) Object ignore) throws SystemException {
        log.trace("AFTER SUCCESS: Transaction successful {}", tm.getTransaction().toString());
    }
}
