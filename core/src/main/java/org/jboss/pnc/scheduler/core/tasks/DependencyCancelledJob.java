package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.api.Dependent;

import javax.transaction.TransactionManager;
import java.util.Set;

public class DependencyCancelledJob extends DependentControllerJob {
    public DependencyCancelledJob(final Set<Dependent> dependents, TransactionManager tm) {
        super(dependents, tm);
    }

    @Override
    void inform(Dependent dependent) {
        dependent.dependencyCancelled();
    }
}
