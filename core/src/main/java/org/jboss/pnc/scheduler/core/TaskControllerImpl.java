package org.jboss.pnc.scheduler.core;

import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.api.DependentMessenger;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.core.jobs.DecreaseCounterJob;
import org.jboss.pnc.scheduler.core.jobs.InvokeStartJob;
import org.jboss.pnc.scheduler.core.jobs.InvokeStopJob;
import org.jboss.pnc.scheduler.core.jobs.ControllerJob;
import org.jboss.pnc.scheduler.core.jobs.DependencyCancelledJob;
import org.jboss.pnc.scheduler.core.jobs.DependencyStoppedJob;
import org.jboss.pnc.scheduler.core.jobs.DependencySucceededJob;
import org.jboss.pnc.scheduler.core.jobs.NotifyCallerJob;
import org.jboss.pnc.scheduler.core.jobs.PokeQueueJob;
import org.jboss.pnc.scheduler.model.ServerResponse;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static javax.transaction.Transactional.TxType.MANDATORY;

@Slf4j
@ApplicationScoped
public class TaskControllerImpl implements TaskController, DependentMessenger {

    private final TaskContainerImpl container;

    private final Event<ControllerJob> scheduleJob;

    public TaskControllerImpl(TaskContainerImpl container, Event<ControllerJob> scheduleJob) {
        this.container = container;
        this.scheduleJob = scheduleJob;
    }

    private List<ControllerJob> transition(Task task) {
        Transition transition;
        transition = getTransition(task);
        if (transition != null)
            log.info("TRANSITION {}: before: {} after: {}", task.getName(), transition.getBefore().toString(), transition.getAfter().toString());

        List<ControllerJob> tasks = new ArrayList<>();

        if (transition == null) {
            return tasks;
        }

        switch (transition) {
            case NEW_to_WAITING:
            case NEW_to_ENQUEUED:
            case WAITING_to_ENQUEUED:
                break;

            case ENQUEUED_to_STARTING:
                tasks.add(new InvokeStartJob(task));
                break;

            case UP_to_STOPPING:
            case STARTING_to_STOPPING:
                tasks.add(new InvokeStopJob(task));
                break;

            case STOPPING_to_STOPPED:
                tasks.add(new DependencyCancelledJob(task));
                tasks.add(new DecreaseCounterJob(task));
                tasks.add(new PokeQueueJob());
                break;

            case NEW_to_STOPPED:
            case WAITING_to_STOPPED:
            case ENQUEUED_to_STOPPED:
                switch (task.getStopFlag()) {
                    case CANCELLED:
                        tasks.add(new DependencyCancelledJob(task));
                        break;
                    case DEPENDENCY_FAILED:
                        tasks.add(new DependencyStoppedJob(task));
                        break;
                }
                break;

            case UP_to_FAILED:
            case STARTING_to_START_FAILED:
            case STOPPING_to_STOP_FAILED:
                tasks.add(new DependencyStoppedJob(task));
                tasks.add(new DecreaseCounterJob(task));
                tasks.add(new PokeQueueJob());
                break;

            case STARTING_to_UP:
                //no tasks
                break;

            case UP_to_SUCCESSFUL:
                tasks.add(new DependencySucceededJob(task));
                tasks.add(new DecreaseCounterJob(task));
                tasks.add(new PokeQueueJob());
                break;
            default:
                throw new IllegalStateException("Controller returned unknown transition: " + transition);
        }
        task.setState(transition.getAfter());

        // notify the caller about a transition
        tasks.add(new NotifyCallerJob(transition, task));
        log.info("SCHEDULE {}: {}", task.getName(), tasks);
        return tasks;
    }

    private Transition getTransition(Task task) {
        Mode mode = task.getControllerMode();
        switch (task.getState()) {
            case NEW: {
                if (shouldStop(task))
                    return Transition.NEW_to_STOPPED;
                if (shouldQueue(task))
                    return Transition.NEW_to_ENQUEUED;
                if (mode == Mode.ACTIVE && task.getUnfinishedDependencies() > 0)
                    return Transition.NEW_to_WAITING;
            }
            case WAITING: {
                if (shouldStop(task))
                    return Transition.WAITING_to_STOPPED;
                if (shouldQueue(task))
                    return Transition.WAITING_to_ENQUEUED;
            }
            case ENQUEUED: {
                if (shouldStop(task))
                    return Transition.ENQUEUED_to_STOPPED;
                if (shouldStart(task))
                    return Transition.ENQUEUED_to_STARTING;
            }
            case STARTING: {
                if (task.getStopFlag() == StopFlag.CANCELLED)
                    return Transition.STARTING_to_STOPPING;
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.STARTING).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.STARTING_to_UP;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    return Transition.STARTING_to_START_FAILED;
            }
            case UP: {
                if (task.getStopFlag() == StopFlag.CANCELLED)
                    return Transition.UP_to_STOPPING;
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.UP).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.UP_to_SUCCESSFUL;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    return Transition.UP_to_FAILED;
            }
            case STOPPING: {
                List<ServerResponse> responses = task.getServerResponses().stream().filter(sr -> sr.getState() == State.STOPPING).collect(Collectors.toList());
                if (responses.stream().anyMatch(ServerResponse::isPositive))
                    return Transition.STOPPING_to_STOPPED;
                if (responses.stream().anyMatch(ServerResponse::isNegative))
                    return Transition.STOPPING_to_STOP_FAILED;
            }
            // final states have no possible transitions
            case START_FAILED:
            case STOP_FAILED:
            case FAILED:
            case SUCCESSFUL:
            case STOPPED:
                break;
            default:
                throw new IllegalStateException("Task is in unrecognized state.");
        }
        return null;
    }

    private boolean shouldQueue(Task task) {
        return task.getControllerMode() == Mode.ACTIVE && task.getUnfinishedDependencies() <= 0;
    }

    private boolean shouldStop(Task task) {
        return task.getStopFlag() != StopFlag.NONE;
    }

    private boolean shouldStart(Task task) {
        return task.getStarting();
    }

    private void handle(MetadataValue<Task> taskMetadata, Task task) {
        handle(taskMetadata, task, null);
    }
    private void handle(MetadataValue<Task> taskMetadata, Task task, ControllerJob[] forcedJobs) {
        List<ControllerJob> jobs = transition(task);
        if (forcedJobs != null && forcedJobs.length != 0) {
            jobs.addAll(Arrays.asList(forcedJobs));
        }
        doExecute(jobs);

        log.debug("SAVE {}: Saving task into ISPN. (ISPN-VERSION: {}) BODY: {}",
                task.getName(),
                taskMetadata.getVersion(),
                task);

        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            log.error("SAVE {}: Concurrent update detected. Transaction will fail.", task.getName());
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    private void doExecute(List<ControllerJob> jobs) {
        for (ControllerJob job : jobs) {
            scheduleJob.fire(job);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void setMode(String name, Mode mode) {
        setMode(name, mode, false);
    }

    @Override
    @Transactional(MANDATORY)
    public void setMode(String name, Mode mode, boolean pokeQueue) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        Mode currentMode = task.getControllerMode();
        if (currentMode == Mode.CANCEL || (mode == Mode.IDLE && currentMode == Mode.ACTIVE)) {
            //no possible movement
            log.error("SET-MODE {}: Incorrect request. (current-mode: {}, proposed-mode: {}) ",
                    name,
                    currentMode,
                    mode);
            return;
        }
        task.setControllerMode(mode);
        if (mode == Mode.CANCEL) {
            task.setStopFlag(StopFlag.CANCELLED);
        }

        // #3 HANDLE
        if (pokeQueue) {
            handle(taskMetadata, task, new ControllerJob[]{new PokeQueueJob()});
        } else {
            handle(taskMetadata, task);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void accept(String name, Object response) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (EnumSet.of(State.STARTING,State.UP,State.STOPPING).contains(task.getState())){
            ServerResponse positiveResponse = new ServerResponse(task.getState(), true, response);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(positiveResponse);
        } else {
            RuntimeException exception = new IllegalStateException("Got response from the remote entity while not in a state to do so. Task: " + task.getName() + " State: " + task.getState());
            log.error("ERROR: ", exception);
            throw exception;
        }

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void fail(String name, Object response) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (EnumSet.of(State.STARTING, State.UP, State.STOPPING).contains(task.getState())){
            ServerResponse negativeResponse = new ServerResponse(task.getState(), false, response);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(negativeResponse);
            task.setServerResponses(responses); //probably unnecessary
            //maybe assert it was NONE before
            task.setStopFlag(StopFlag.UNSUCCESSFUL);
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Task: " + task.getName() + " State: " + task.getState());
        }

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dequeue(String name) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        if (task.getState() == State.ENQUEUED) {
            task.setStarting(true);
        } else {
            throw new IllegalStateException("Attempting to dequeue while not in a state to do. Task: " + task.getName() + " State: " + task.getState());
        }

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencySucceeded(String name) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        task.decUnfinishedDependencies();

        // #3 HANDLE
        handle(taskMetadata, task);
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyStopped(String name) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        //maybe assert it was NONE before
        task.setStopFlag(StopFlag.DEPENDENCY_FAILED);

        // #3 HANDLE
        handle(taskMetadata, task);
    }


    @Override
    @Transactional(MANDATORY)
    public void dependencyCancelled(String name) {
        // #1 PULL
        MetadataValue<Task> taskMetadata = container.getRequiredTaskWithMetadata(name);
        Task task = taskMetadata.getValue();

        // #2 ALTER
        //maybe assert it was NONE before
        task.setStopFlag(StopFlag.CANCELLED);

        // #3 HANDLE
        handle(taskMetadata, task);
    }
}