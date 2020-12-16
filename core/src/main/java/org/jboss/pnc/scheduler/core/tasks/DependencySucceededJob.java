package org.jboss.pnc.scheduler.core.tasks;

import javax.enterprise.event.TransactionPhase;
import java.util.Set;

public class DependencySucceededJob extends DependentControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencySucceededJob(Set<String> dependents) {
        super(dependents, INVOCATION_PHASE);
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencySucceeded(dependentName);
    }
}
