package org.jboss.pnc.scheduler.core.delegates;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.api.TaskController;

@WithTransactions
@ApplicationScoped
public class TransactionalTaskController implements TaskController {

    TaskController delegate;

    TransactionManager manager;

    public TransactionalTaskController(TaskController controller, TransactionManager manager) {
        this.delegate = controller;
        this.manager = manager;
    }

    @Override
    @Transactional
    public void setMode(String name, Mode mode) {
        delegate.setMode(name, mode);
    }

    @Override
    @Transactional
    public void accept(String name) {
        delegate.accept(name);
    }

    @Override
    @Transactional
    public void fail(String name) {
        delegate.fail(name);
    }
}
