package org.jboss.pnc.scheduler.core.tasks;

import javax.enterprise.event.TransactionPhase;
import java.util.Set;

public class DependencyCancelledJob extends DependentControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencyCancelledJob(Set<String> dependents) {
        super(dependents, INVOCATION_PHASE);
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencyCancelled(dependentName);
    }
}
