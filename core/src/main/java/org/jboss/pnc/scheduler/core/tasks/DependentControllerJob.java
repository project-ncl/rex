package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.api.Dependent;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;
import java.util.Set;

public abstract class DependentControllerJob extends TransactionalControllerJob {

    private final Set<String> dependents;

    protected Dependent dependentAPI;

    protected DependentControllerJob(Set<String> dependents, TransactionPhase invocationPhase) {
        super(invocationPhase);
        this.dependents = dependents;
        this.dependentAPI = CDI.current().select(Dependent.class).get();
    }

    @Override
    boolean execute() {
        for (String dependent : dependents) {
            inform(dependent);
        }
        return true;
    }

    abstract void inform(final String dependentName);
}
