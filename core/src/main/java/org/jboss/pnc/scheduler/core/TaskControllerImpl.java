package org.jboss.pnc.scheduler.core;

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
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static javax.transaction.Transactional.TxType.MANDATORY;

@ApplicationScoped
public class TaskControllerImpl implements TaskController, DependentMessenger {

    private static final Logger logger = LoggerFactory.getLogger(TaskControllerImpl.class);

    private final TaskContainerImpl container;

    private final Event<ControllerJob> scheduleJob;

    public TaskControllerImpl(TaskContainerImpl container, Event<ControllerJob> scheduleJob) {
        this.container = container;
        this.scheduleJob = scheduleJob;
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyCreated(String name, String dependencyName) {
        MetadataValue<Task> taskMeta = container.getCache().getWithMetadata(name);
        assertNotNull(taskMeta, new TaskNotFoundException("Task " + name + " not found!"));
        Task task = taskMeta.getValue();
        Task dependency = container.getTask(dependencyName);

        newDependency(task, dependency);

        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
        //Get get controller of the dependency and notify it that it has a new dependant
        this.dependantCreated(dependencyName, name);
    }

    public static void newDependency(Task dependant, Task dependency) {
        assertTaskNotNull(dependant, dependency);
        assertDependantRelationships(dependant, dependency);
        assertCanAcceptDependencies(dependant);
        //if the supposed new dependency didn't finish, increase unfinishedDependencies counter
        dependant.getDependencies().add(dependency.getName());
        if (!dependency.getState().isFinal()) {
            dependant.incUnfinishedDependencies();
        }
    }

    public static void newDependant(Task dependency, Task dependant) {
        assertTaskNotNull(dependant, dependency);
        assertDependencyRelationships(dependency, dependant);
        assertCanAcceptDependencies(dependant);
        dependency.getDependants().add(dependant.getName());
    }

    @Transactional(MANDATORY)
    public void dependantCreated(String name, String dependantName) {
        MetadataValue<Task> taskMeta = container.getCache().getWithMetadata(name);
        assertNotNull(taskMeta, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMeta.getValue();
        Task dependant = container.getTask(dependantName);

        newDependant(task, dependant);

        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Transactional(MANDATORY)
    private List<ControllerJob> transition(Task task) {
        Transition transition;
        transition = getTransition(task);
        if (transition != null)
            logger.info(String.format("Transitioning: before: %s after: %s for task: %s", transition.getBefore().toString(),transition.getAfter().toString(), task.getName()));

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
                tasks.add(new DecreaseCounterJob());
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
                tasks.add(new DecreaseCounterJob());
                tasks.add(new PokeQueueJob());
                break;

            case STARTING_to_UP:
                //no tasks
                break;

            case UP_to_SUCCESSFUL:
                tasks.add(new DependencySucceededJob(task));
                tasks.add(new DecreaseCounterJob());
                tasks.add(new PokeQueueJob());
                break;
            default:
                throw new IllegalStateException("Controller returned unknown transition: " + transition);
        }
        task.setState(transition.getAfter());

        // notify the caller about a transition
        tasks.add(new NotifyCallerJob(transition, task));
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

    private static void assertCanAcceptDependencies(Task task) {
        if (!task.getState().isIdle()) {
            throw new IllegalStateException(String.format("Task %s cannot accept a dependency",
                    task.getName()));
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
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        Mode currentMode = task.getControllerMode();
        if (currentMode == Mode.CANCEL || (mode == Mode.IDLE && currentMode == Mode.ACTIVE)) {
            //no possible movement
            //TODO log
            return;
        }
        task.setControllerMode(mode);
        if (mode == Mode.CANCEL) {
            task.setStopFlag(StopFlag.CANCELLED);
        }

        List<ControllerJob> tasks = transition(task);
        if (pokeQueue) {
            tasks.add(new PokeQueueJob());
        }
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void accept(String name, Object response) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        if (EnumSet.of(State.STARTING,State.UP,State.STOPPING).contains(task.getState())){
            ServerResponse positiveResponse = new ServerResponse(task.getState(), true, response);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(positiveResponse);
            task.setServerResponses(responses); //probably unnecessary
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Task: " + task.getName() + " State: " + task.getState());
        }

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    private void doExecute(List<ControllerJob> tasks) {
        for (ControllerJob task : tasks) {
            scheduleJob.fire(task);
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void fail(String name, Object response) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Service " + name + "not found"));
        Task task = taskMetadata.getValue();

        if (EnumSet.of(State.STARTING, State.UP, State.STOPPING).contains(task.getState())){
            ServerResponse negativeResponse = new ServerResponse(task.getState(), false, response);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(negativeResponse);
            task.setServerResponses(responses); //probably unnecessary
            //maybe assert it was null before
            task.setStopFlag(StopFlag.UNSUCCESSFUL);
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Service: " + task.getName() + " State: " + task.getState());
        }

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void dequeue(String name) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Service " + name + "not found"));
        Task task = taskMetadata.getValue();

        if (task.getState() == State.ENQUEUED) {
            task.setStarting(true);
        } else {
            throw new IllegalStateException("Attempting to dequeue while not in a state to do. Service: " + task.getName() + " State: " + task.getState());
        }

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencySucceeded(String name) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.decUnfinishedDependencies();

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        logger.info("Called dep succeeded on " + name + " and pushed: " + pushed + " with prev version: " + taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    @Transactional(MANDATORY)
    public void dependencyStopped(String name) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.setStopFlag(StopFlag.DEPENDENCY_FAILED);

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }


    @Override
    @Transactional(MANDATORY)
    public void dependencyCancelled(String name) {
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.setStopFlag(StopFlag.CANCELLED);

        List<ControllerJob> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    private static void assertDependantRelationships(Task dependant, Task dependency){
        String dependantName = dependant.getName();
        String dependencyName = dependency.getName();

        if (dependantName.equals(dependencyName)) {
            throw new IllegalStateException("Task " + dependantName + " cannot depend on itself");
        };

        if (dependant.getDependants().contains(dependencyName)) {
            throw new IllegalStateException("Task " + dependantName + " cannot depend and be dependant on the "+ dependencyName);
        }
    };

    private static void assertDependencyRelationships(Task dependency, Task dependant){
        String dependencyName = dependency.getName();
        String dependantName = dependant.getName();

        if (dependantName.equals(dependencyName)) {
            throw new IllegalStateException("Task " + dependencyName + " cannot depend on itself");
        };

        if (dependant.getDependants().contains(dependencyName)) {
            throw new IllegalStateException("Task " + dependencyName + " cannot depend and be dependant on the "+ dependantName);
        }
    };

    private static void assertTaskNotNull(Task... tasks) {
        for (Task task : tasks) {
            assertNotNull(task, new TaskNotFoundException("Task " + task.getName() + "was not found!"));
        }
    }

    private static <T> T assertNotNull(T object) {
        return assertNotNull(object, new IllegalArgumentException("Parameter of class: "+ object.getClass().getCanonicalName() + " cannot be null."));
    }

    private static <T> T assertNotNull(T object, RuntimeException e) {
        if (object == null) {
            throw e;
        }
        return object;
    }

}
