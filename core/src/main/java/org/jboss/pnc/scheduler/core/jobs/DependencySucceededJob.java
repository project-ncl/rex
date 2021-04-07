package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;

public class DependencySucceededJob extends DependentControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencySucceededJob(Task task) {
        super(task, INVOCATION_PHASE);
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencySucceeded(dependentName);
    }
}
