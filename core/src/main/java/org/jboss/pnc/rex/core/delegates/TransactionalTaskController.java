/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.delegates;

import java.time.Instant;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;
import org.jboss.pnc.rex.common.enums.ResponseFlag;
import org.jboss.pnc.rex.core.api.TaskController;

import io.quarkus.arc.Unremovable;

@WithTransactions
@Unremovable
@ApplicationScoped
@Transactional
public class TransactionalTaskController implements TaskController {

    private final TaskController delegate;

    public TransactionalTaskController(TaskController controller) {
        this.delegate = controller;
    }

    @Override
    public void setMode(String name, Mode mode) {
        delegate.setMode(name, mode);
    }

    @Override
    public void setMode(String name, Mode mode, boolean pokeQueue) {
        delegate.setMode(name, mode, pokeQueue);
    }

    @Override
    public void accept(String name, Object response, Origin origin, boolean isRollback, Set<ResponseFlag> flags) {
        delegate.accept(name, response, origin, isRollback, flags);
    }

    @Override
    public void fail(String name, Object response, Origin origin, boolean isRollback, Set<ResponseFlag> flags) {
        delegate.fail(name, response, origin, isRollback, flags);
    }

    @Override
    public void beat(String name, Object response, Instant beatTime) {
        delegate.beat(name, response, beatTime);
    }

    @Override
    public void dequeue(String name) {
        delegate.dequeue(name);
    }

    @Override
    public void delete(String name) {
        delegate.delete(name);
    }

    @Override
    public void markForDisposal(String name, boolean pokeCleaner) {
        delegate.markForDisposal(name, pokeCleaner);
    }

    @Override
    public void clearConstraint(String name) {
        delegate.clearConstraint(name);
    }

    @Override
    public void reset(String name) {
        this.delegate.reset(name);
    }

    @Override
    public void primeForRollback(String name, int rollbackDependants, int dependencies) {
        delegate.primeForRollback(name, rollbackDependants, dependencies);
    }

    @Override
    public void rollbackTriggered(String name) {
        delegate.rollbackTriggered(name);
    }

    @Override
    public void startRollbackProcess(String name) {
        delegate.startRollbackProcess(name);
    }

    @Override
    public void involveInTransaction(String name) {
        delegate.involveInTransaction(name);
    }
}
