package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.api.Dependent;

import javax.transaction.TransactionManager;
import java.util.Set;

public abstract class DependentControllerTask extends TransactionalControllerTask {

    Set<Dependent> dependents;

    DependentControllerTask(final Set<Dependent> dependents, TransactionManager tm) {
        super(tm);
        this.dependents = dependents;
    }

    @Override
    boolean execute() {
        for (Dependent dependent : dependents) {
            inform(dependent);
        }
        return true;
    }

    abstract void inform(final Dependent dependent);
}
