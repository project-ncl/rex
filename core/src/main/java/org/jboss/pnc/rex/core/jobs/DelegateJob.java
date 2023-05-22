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

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.TransactionSemantics;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.model.Task;

import javax.enterprise.event.TransactionPhase;

@Slf4j
public class DelegateJob extends ControllerJob {

    private final ControllerJob delegate;

    private final boolean tolerant;

    private final TransactionSemantics txType;

    private final boolean transactional;

    private DelegateJob(TransactionPhase invocationPhase,
                        Task context,
                        boolean async,
                        boolean tolerant,
                        boolean transactional,
                        TransactionSemantics transactionSemantics,
                        ControllerJob delegate) {
        super(invocationPhase, context, async);
        this.delegate = delegate;
        this.tolerant = tolerant;
        this.txType = transactionSemantics;
        this.transactional = transactional;
        if (transactional && transactionSemantics == null) {
            throw new IllegalArgumentException("Transaction semantics null while delegate declared transactional");
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
                .onItem()
                .transform(this::delegateExecute);

        if (tolerant) {
            uni = uni.onFailure().retry().atMost(15);
        }

        return uni.await().indefinitely();
    }

    boolean delegateExecute(Void ignore) {
        if (!transactional) {
            return delegate.execute();
        }

        return QuarkusTransaction.runner(txType).call(delegate::execute);
    }

    @Override
    void onFailure() {delegate.onFailure();}

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
        private TransactionSemantics semantics;

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

        public DelegateJob.DelegateJobBuilder transactionSemantics(TransactionSemantics semantics) {
            this.semantics = semantics;
            return this;
        }

        public DelegateJob.DelegateJobBuilder delegate(ControllerJob delegate) {
            this.delegate = delegate;
            return this;
        }

        public DelegateJob build() {
            return new DelegateJob(invocationPhase, context, async, tolerant, transactional, semantics, delegate);
        }
    }
}

