package org.jboss.pnc.scheduler.core.tasks;

import java.util.Set;

public abstract class DependentControllerJob extends TransactionalControllerJob {

    Set<String> dependents;

    DependentControllerJob(Set<String> dependents) {
        super();
        this.dependents = dependents;
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
