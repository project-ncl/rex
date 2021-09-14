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
package org.jboss.pnc.rex.core.jobs;

import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.model.Task;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@Slf4j
public class DelegateJob extends ControllerJob {

    private final ControllerJob delegate;

    private final boolean tolerant;

    private final TransactionManager manager;

    private final boolean alreadyInTransaction;

    private final boolean transactional;

    private DelegateJob(TransactionPhase invocationPhase, Task context, boolean async, boolean tolerant, boolean transactional, ControllerJob delegate) {
        super(invocationPhase, context, async);
        this.delegate = delegate;
        this.tolerant = tolerant;
        this.transactional = transactional;
        this.manager = CDI.current().select(TransactionManager.class).get();
        try {
            this.alreadyInTransaction = !async && manager.getTransaction() != null;
        } catch (SystemException e) {
            throw new IllegalStateException("SystemError", e);
        }
    }

    @Override
    void beforeExecute() {
        delegate.beforeExecute();
    }

    @Override
    void afterExecute() {
        delegate.afterExecute();
    }

    @Override
    boolean execute() {
        Uni<Boolean> uni = Uni.createFrom().voidItem()
                .onItem().transform(ignore -> {
                    boolean ret = false;
                    try {
                        startOrJoin();
                        ret = delegate.execute();
                        commit();
                    } catch (RollbackException e) {
                        throw new IllegalStateException("Transaction rolled back", e);
                    } catch (Exception e) {
                        rollback(e);
                        throw e;
                    }
                    return ret;
                });

        if (tolerant) {
            uni = uni.onFailure().retry().atMost(15);
        }

        return uni.await().indefinitely();
    }

    void startOrJoin() {
        if (!transactional) {
            return;
        }
        if (!alreadyInTransaction) {
            try {
                manager.begin();
            } catch (NotSupportedException | SystemException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown", e);
            }
        }
    }

    void rollback(Throwable e) {
        if (!transactional) {
            return;
        }
        try {
            //transaction initiator should handle the rollback
            if (!alreadyInTransaction) manager.rollback();
        } catch (SystemException ex) {
            throw new IllegalStateException("Cannot rollback Transaction, unexpected error was thrown", ex);
        }
    }

    void commit() throws RollbackException {
        if (!transactional) {
            return;
        }
        if (!alreadyInTransaction) {
            try {
                manager.commit();
            } catch (SystemException | HeuristicRollbackException | HeuristicMixedException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Unexpected error was thrown while committing transaction", e);
            }
        }
    }

    @Override
    void onException(Throwable e) {
        delegate.onException(e);
    }

    public static DelegateJob.DelegateJobBuilder builder() {
        return new DelegateJob.DelegateJobBuilder();
    }

    public static class DelegateJobBuilder {
        private TransactionPhase invocationPhase;
        private Task context;
        private boolean async;
        private ControllerJob delegate;
        private boolean tolerant;
        private boolean transactional;

        DelegateJobBuilder() {
        }

        public DelegateJob.DelegateJobBuilder invocationPhase(TransactionPhase invocationPhase) {
            this.invocationPhase = invocationPhase;
            return this;
        }

        public DelegateJob.DelegateJobBuilder tolerant(boolean tolerant) {
            this.tolerant = tolerant;
            return this;
        }

        public DelegateJob.DelegateJobBuilder context(Task context) {
            this.context = context;
            return this;
        }

        public DelegateJob.DelegateJobBuilder async(boolean async) {
            this.async = async;
            return this;
        }
        public DelegateJob.DelegateJobBuilder transactional(boolean transactional) {
            this.transactional = transactional;
            return this;
        }

        public DelegateJob.DelegateJobBuilder delegate(ControllerJob delegate) {
            this.delegate = delegate;
            return this;
        }

        public DelegateJob build() {
            return new DelegateJob(invocationPhase, context, async, tolerant, transactional, delegate);
        }
    }

    public static void main(String[] args) {
//        Uni.createFrom().item(DelegateJob::method)
//                .invoke(DelegateJob::method2)
//                .onItem().invoke(() -> System.out.println("!"))
//                .onFailure().retry()
//                .atMost(16)
//                .await().indefinitely();
        boolean tolerant = true;
        Uni<Object> uni = Uni.createFrom().voidItem()
                .invoke(DelegateJob::method)
                .onItem().transformToUni(ignore -> Uni.createFrom().item(DelegateJob::executoring))
                .invoke(DelegateJob::method2)
                .onFailure().invoke(DelegateJob::method3);
        if (tolerant) {
            uni = uni.onFailure().invoke(() -> System.out.println()).onFailure().retry().atMost(15);
        }
        uni.await().indefinitely();
    }

    private static Object executoring() {
        System.out.println("executing delegate");
                throw new IllegalArgumentException();
//        return null;
    }

    private static Object method3() {
        System.out.println("would rollback");

        return null;
    }

    private static Object method() {
        System.out.println("would start transaction");

//        throw new IllegalArgumentException();
        return Object.class;
    }
    private static Object method2() {
        System.out.println("would commit");

//        throw new IllegalArgumentException();
        return Object.class;
    }
}

