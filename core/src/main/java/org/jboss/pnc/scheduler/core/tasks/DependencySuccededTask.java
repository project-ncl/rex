package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.api.Dependent;

import javax.transaction.TransactionManager;
import java.util.Set;

public class DependencySuccededTask extends  DependentControllerTask {
    public DependencySuccededTask(Set<Dependent> dependents, TransactionManager tm) {
        super(dependents, tm);
    }

    @Override
    void inform(Dependent dependent) {
        dependent.dependencySucceeded();
    }
}
