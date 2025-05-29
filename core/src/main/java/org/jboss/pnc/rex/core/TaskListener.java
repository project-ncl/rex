/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
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

    @Inject
    TransactionManager tm;

    @Inject
    ManagedExecutor executor;

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
