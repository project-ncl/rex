/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.jobs;

import jakarta.enterprise.event.TransactionPhase;

import org.jboss.pnc.rex.core.delegates.FaultToleranceDecorator;
import org.jboss.pnc.rex.model.Task;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.TransactionSemantics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateJob extends ControllerJob {

    private final ControllerJob delegate;

    private final boolean tolerant;

    private final TransactionSemantics txType;

    private final boolean transactional;

    private final FaultToleranceDecorator ft;

    private DelegateJob(
            TransactionPhase invocationPhase,
            Task context,
            boolean async,
            boolean tolerant,
            boolean transactional,
            TransactionSemantics transactionSemantics,
            ControllerJob delegate,
            FaultToleranceDecorator ft) {
        super(invocationPhase, context, async);
        this.delegate = delegate;
        this.tolerant = tolerant;
        this.txType = transactionSemantics;
        this.transactional = transactional;
        this.ft = ft;
        if (transactional && transactionSemantics == null) {
            throw new IllegalArgumentException("Transaction semantics null while delegate declared transactional");
        }
        if (tolerant && ft == null) {
            throw new IllegalArgumentException("Must provide FT decorator if tolerant.");
        }
    }

    @Override
    protected void beforeExecute() {
        delegate.beforeExecute();
    }

    @Override
    protected void afterExecute() {
        delegate.afterExecute();
    }

    @Override
    public boolean execute() {
        if (tolerant) {
            return ft.withTolerance(this::delegateExecute);
        }

        return delegateExecute();
    }

    boolean delegateExecute() {
        if (transactional) {
            return QuarkusTransaction.runner(txType).call(delegate::execute);
        }

        return delegate.execute();
    }

    @Override
    protected void onFailure() {
        delegate.onFailure();
    }

    @Override
    protected void onException(Throwable e) {
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
        private FaultToleranceDecorator ft;

        DelegateJobBuilder() {
        }

        public DelegateJob.DelegateJobBuilder invocationPhase(TransactionPhase invocationPhase) {
            this.invocationPhase = invocationPhase;
            return this;
        }

        public DelegateJob.DelegateJobBuilder tolerant(boolean tolerant, FaultToleranceDecorator ft) {
            this.tolerant = tolerant;
            this.ft = ft;
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
            return new DelegateJob(invocationPhase, context, async, tolerant, transactional, semantics, delegate, ft);
        }
    }

    @Override
    public String toString() {
        return delegate.toString(context, async, invocationPhase) + appendDelegateInfo();
    }

    private String appendDelegateInfo() {
        if (transactional || tolerant) {
            return " + [" + (transactional ? "inner TXs" : "") + (tolerant ? ", fault-tolerant" : "") + "]";
        } else {
            return "";
        }
    }

}
