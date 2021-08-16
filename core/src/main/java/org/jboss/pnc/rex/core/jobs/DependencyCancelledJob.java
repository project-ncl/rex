package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.model.Task;

import javax.enterprise.event.TransactionPhase;

public class DependencyCancelledJob extends DependentControllerJob {

    private static final TransactionPhase INVOCATION_PHASE = TransactionPhase.IN_PROGRESS;

    public DependencyCancelledJob(Task task) {
        super(task, INVOCATION_PHASE);
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencyCancelled(dependentName);
    }
}
