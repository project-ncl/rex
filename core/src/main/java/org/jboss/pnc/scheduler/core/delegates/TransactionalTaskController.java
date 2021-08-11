package org.jboss.pnc.scheduler.core.delegates;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.api.TaskController;

@WithTransactions
@ApplicationScoped
public class TransactionalTaskController implements TaskController {

    private final TaskController delegate;

    public TransactionalTaskController(TaskController controller) {
        this.delegate = controller;
    }

    @Override
    @Transactional
    public void setMode(String name, Mode mode) {
        delegate.setMode(name, mode);
    }

    @Override
    @Transactional
    public void setMode(String name, Mode mode, boolean pokeQueue) {
        delegate.setMode(name, mode, pokeQueue);
    }

    @Override
    @Transactional
    public void accept(String name, Object response) {
        delegate.accept(name, response);
    }

    @Override
    @Transactional
    public void fail(String name, Object response) {
        delegate.fail(name, response);
    }

    @Override
    @Transactional
    public void dequeue(String name) {
        delegate.dequeue(name);
    }
}
