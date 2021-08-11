package org.jboss.pnc.scheduler.core.jobs;

import org.jboss.pnc.scheduler.core.api.DependentMessenger;
import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.CDI;
import java.util.Set;

/**
 * Jobs implementing this abstract class are used for messaging all dependants about important changes(dependency has
 * finished, failed...)
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public abstract class DependentControllerJob extends ControllerJob {

    private final Set<String> dependents;

    protected DependentMessenger dependentAPI;

    protected DependentControllerJob(Task task, TransactionPhase invocationPhase) {
        super(invocationPhase, task);
        this.dependents = task.getDependants();
        this.dependentAPI = CDI.current().select(DependentMessenger.class).get();
    }

    @Override
    boolean execute() {
        for (String dependent : dependents) {
            inform(dependent);
        }
        return true;
    }

    abstract void inform(final String dependentName);

    @Override
    void beforeExecute() {}

    @Override
    void afterExecute() {}

    @Override
    void onException(Throwable e) {}
}
