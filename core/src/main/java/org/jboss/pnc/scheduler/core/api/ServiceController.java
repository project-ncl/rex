package org.jboss.pnc.scheduler.core.api;

import org.jboss.msc.service.ServiceName;

/**
 * This is API for ServiceController.
 * <p>
 * ServiceController is an main entity that manipulates and handles transitions for each scheduled remote job(Service).
 *
 * <p>
 * ServiceController does not hold any data besides ServiceName key of a Service that it is associated with. Before each method
 * is invoked it loads updated data from Container.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface ServiceController {
    /**
     * Gets unique name of an associated job(Service).
     *
     * @return the name
     */
    ServiceName getName();

    /**
     * Gets the Container in which the job(Service) is installed in.
     *
     * @return the container
     */
    ServiceContainer getContainer();

    /**
     * Gets mode of a Service.
     *
     * @return the mode
     */
    Mode getMode();

    /**
     * Sets mode of a Service. Needs to be called under a lock.
     *
     * @param mode the mode
     */
    void setMode(Mode mode);

    /**
     * Gets current Service state.
     *
     * @return the state
     */
    State getState();

    /**
     * Method used for positive callback. Needs to be called under a lock.
     *
     * f.e. to signalize that remote job(Service) has started/cancelled/finished.
     */
    void accept();

    /**
     * Method used for negative callback. Needs to be called under a lock.
     *
     * f.e. to signalize that remote job(Service) failed to start/cancel or failed during execution.
     */
    void fail();

    /**
     * ServiceControllers communicate with each other through Tasks. Modes serve as a way for users and other entities to affect
     * Controller's behaviour.
     */
    enum Mode {
        /**
         * Controller does not attempt to start and sits idly. This is initial Mode.
         */
        IDLE,
        /**
         * Controller is actively trying to start its execution.
         */
        ACTIVE,
        /**
         * Controller is told to cancel its execution.
         */
        CANCEL
    }

    /**
     * The enum represents State the job(Service) is currently in.
     * <p>
     * State represents vertices in state-machine diagram.
     */
    enum State {

        /**
         * Service was created and is being idle. It does not transition unless Mode.ACTIVE.
         */
        NEW(StateGroup.IDLE),
        /**
         * Controller is waiting for either all dependencies to successfully complete
         * or for room in the Container(limited number of active tasks).
         */
        WAITING(StateGroup.IDLE),
        /**
         * Controller requests remote job(Service) to start and waits for callback to approve that remote job successfully started.
         */
        STARTING(StateGroup.RUNNING),
        /**
         * Remote job(Service) successfully started and is running.
         */
        UP(StateGroup.RUNNING),
        /**
         * Controller requests remote job(Service) to stop and waits for a callback to approve that remote job successfully stopped.
         */
        STOPPING(StateGroup.RUNNING),
        /**
         * Received callback that remote job(Service) failed to start.
         */
        START_FAILED(StateGroup.FINAL),
        /**
         * Received callback that remote job(Service) failed to stop.
         */
        STOP_FAILED(StateGroup.FINAL),
        /**
         * Remote job(Service) failed during execution.
         */
        FAILED(StateGroup.FINAL),
        /**
         * Remote job(Service) ended successfully.
         */
        SUCCESSFUL(StateGroup.FINAL),
        /**
         * Remote job(Service) stopped successfully.
         */
        STOPPED(StateGroup.FINAL);

        private final StateGroup type;

        State(StateGroup type) {
            this.type = type;
        }

        public StateGroup getGroup() {
            return type;
        }

        public boolean isIdle() {
            return type.equals(StateGroup.IDLE);
        }

        public boolean isRunning() {
            return type.equals(StateGroup.IDLE);
        }

        public boolean isFinal() {
            return type.equals(StateGroup.IDLE);
        }
    }

    /**
     * The enum State Group.
     */
    enum StateGroup {
        /**
         * Controller is idle and job(Service) hasn't started remote execution.
         * <p>
         * In this state you are able to add additional dependencies.
         */
        IDLE,
        /**
         * Job(Service) is remotely active.
         * <p>
         * In this state you are unable to add additional dependencies.
         */
        RUNNING,
        /**
         * Job(Service) has finished execution or failed.
         * <p>
         * In this state you are unable to add additional dependencies and cannot transition to any other state.
         */
        FINAL
    }

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
    enum Transition {
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
        private final State after;

        Transition(State before, State after) {
            this.before = before;
            this.after = after;
        }
    }
}