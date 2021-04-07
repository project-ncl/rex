package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;
import java.util.Set;

public class DependencyStoppedJob extends DependentControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencyStoppedJob(Set<String> dependents) {
        super(dependents, INVOCATION_PHASE);
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencyStopped(dependentName);
    }
}
