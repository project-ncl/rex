package org.jboss.pnc.scheduler.common.enums;

/**
 * This enum represent Transition between States
 *
 * Transition is a process, that means that to complete transition there could be number of tasks that need to be executed
 * before a transition is completed. Another transition cannot be initiated until these tasks are completed.
 *
 * F.e. Transition between UP and STOPPING state creates x tasks for each dependant to stop and one task to invoke async
 * http server to stop remote execution of itself. After these tasks are completed, t can Transition to STOPPED/STOP_FAILED
 * which is dependent on positive or negative callback.
 *
 * Transition is an edge in state-machine diagram.
 */
public enum Transition {
    /**
     * Created job(Service) is set to Mode.ACTIVE and has unfinished dependencies or Container has no room for an active job.
     */
    NEW_to_WAITING(State.NEW, State.WAITING),
    /**
     * Created job(Service) is set to Mode.ACTIVE, has no unfinished dependencies and Container has room for an active job.
     *
     * Controller invokes async http client to start execution of remote job(Service).
     */
    NEW_to_STARTING(State.NEW, State.STARTING),
    /**
     * Controller has dependencies successfully finished and Container had room for an active job.
     *
     * Controller invokes async http client to start execution of remote job(Service).
     */
    WAITING_to_STARTING(State.WAITING, State.STARTING),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller invokes async http client to stop execution of remote job(Service) and informs dependants that it's stopping.
     */
    UP_to_STOPPING(State.UP,State.STOPPING),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller invokes async http client to stop execution of remote job(Service) and informs dependants that it's stopping.
     */
    STARTING_to_STOPPING(State.STARTING, State.STOPPING),
    /**
     * Controller has received positive callback that remote job(Service) stopped its execution.
     */
    STOPPING_to_STOPPED(State.STOPPING, State.STOPPED),
    /**
     * Controller has received negative callback and remote job(Service) failed to stop(could be f.e. unavailable).
     *
     * Controller informs its dependants that it failed.
     */
    STOPPING_to_STOP_FAILED(State.STOPPING,State.STOP_FAILED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs its dependants that it stopped.
     */
    NEW_to_STOPPED(State.NEW, State.STOPPED),
    /**
     * User has set Controllers mode to Mode.CANCEL.
     *
     * Controller informs its dependants that it stopped.
     */
    WAITING_to_STOPPED(State.WAITING, State.STOPPED),
    /**
     * Controller received positive callback that remote job(Service) has successfully started its execution.
     */
    STARTING_to_UP(State.STARTING, State.UP),
    /**
     * Controller received negative callback that remote job(Service) failed to start its execution.
     *
     * Controller informs its dependants that it failed.
     */
    STARTING_to_START_FAILED(State.STARTING, State.START_FAILED),
    /**
     * Controller received an notification that remote job(Service) failed during its execution.
     *
     * Controller informs its dependants that it failed.
     */
    UP_to_FAILED(State.UP, State.FAILED),
    /**
     * Controller received an notification that remote job(Service) has successfully completed its execution.
     *
     * Controller informs its dependants that it successfully finished.
     */
    UP_to_SUCCESSFUL(State.UP, State.SUCCESSFUL);

    private final State before;

    public State getBefore() {
        return before;
    }

    public State getAfter() {
        return after;
    }

    private final State after;

    Transition(State before, State after) {
        this.before = before;
        this.after = after;
    }
}
