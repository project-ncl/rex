package org.jboss.pnc.scheduler.core;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.common.enums.Transition;
import org.jboss.pnc.scheduler.core.api.Dependent;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.common.exceptions.TaskNotFoundException;
import org.jboss.pnc.scheduler.core.tasks.*;
import org.jboss.pnc.scheduler.model.ServerResponse;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class TaskControllerImpl implements TaskController, Dependent {

    private static final Logger logger = LoggerFactory.getLogger(TaskControllerImpl.class);

    private String name;

    private TaskContainerImpl container;

    private TransactionManager tm;

    public TaskControllerImpl(String name, TaskContainerImpl container) {
        this.name = name;
        this.container = container;
        tm = container.getTransactionManager();
    }

    @Override
    public void dependencyCreated(String dependencyName) {
        assertInTransaction();
        MetadataValue<Task> taskMeta = container.getCache().getWithMetadata(name);
        assertNotNull(taskMeta, new TaskNotFoundException("Task " + name + " not found!"));
        Task task = taskMeta.getValue();
        Task dependency = container.getTask(dependencyName);

        newDependency(task, dependency);

        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task,taskMeta.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
        //Get get controller of the dependency and notify it that it has a new dependant
        TaskControllerImpl dependencyController = (TaskControllerImpl) container.getTaskController(dependencyName);
        dependencyController.dependantCreated(name);
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

    public void dependantCreated(String dependantName) {
        assertInTransaction();
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

    private List<Runnable> transition(Task task) {
        assertInTransaction();
        Transition transition;
        transition = getTransition(task);
        if (transition != null)
            System.out.println(String.format("Transitioning: before: %s after: %s for task: %s", transition.getBefore().toString(),transition.getAfter().toString(), getName()));

        List<Runnable> tasks = new ArrayList<>();

        if (transition == null) {
            return tasks;
        }

        switch (transition) {
            case NEW_to_WAITING:
                //no tasks
                break;

            case NEW_to_STARTING:
            case WAITING_to_STARTING:
                tasks.add(new AsyncInvokeStartJob(tm, task, this, container.getBaseUrl()));
                break;

            case UP_to_STOPPING:
            case STARTING_to_STOPPING:
                tasks.add(new AsyncInvokeStopJob(tm, task, this, container.getBaseUrl()));
                break;

            case STOPPING_to_STOPPED:
                tasks.add(new DependencyCancelledJob(
                        task.getDependants().stream().map(dep -> container.getTaskControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;

            case NEW_to_STOPPED:
            case WAITING_to_STOPPED:
                switch (task.getStopFlag()) {
                    case CANCELLED:
                        tasks.add(new DependencyCancelledJob(
                                task.getDependants().stream().map(dep -> container.getTaskControllerInternal(dep)).collect(Collectors.toSet()),
                                tm));
                        break;
                    case DEPENDENCY_FAILED:
                        tasks.add(new DependencyStoppedJob(
                                task.getDependants().stream().map(dep -> container.getTaskControllerInternal(dep)).collect(Collectors.toSet()),
                                tm));
                        break;

                };

            case UP_to_FAILED:
            case STARTING_to_START_FAILED:
            case STOPPING_to_STOP_FAILED:
                tasks.add(new DependencyStoppedJob(
                        task.getDependants().stream().map(dep -> container.getTaskControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;

            case STARTING_to_UP:
                //no tasks
                break;

            case UP_to_SUCCESSFUL:
                tasks.add(new DependencySucceededJob(
                        task.getDependants().stream().map(dep -> container.getTaskControllerInternal(dep)).collect(Collectors.toSet()),
                        tm));
                break;
            default:
                throw new IllegalStateException("Controller returned unknown transition: " + transition);
        }
        task.setState(transition.getAfter());
        return tasks;
    }

    private Transition getTransition(Task task) {
        Mode mode = task.getControllerMode();
        switch (task.getState()) {
            case NEW: {
                if (shouldStop(task))
                    return Transition.NEW_to_STOPPED;
                if (shouldStart(task))
                    return Transition.NEW_to_STARTING;
                if (mode == Mode.ACTIVE && task.getUnfinishedDependencies() > 0)
                    return Transition.NEW_to_WAITING;
            }
            case WAITING: {
                if (shouldStop(task))
                    return Transition.WAITING_to_STOPPED;
                if (shouldStart(task))
                    return Transition.WAITING_to_STARTING;
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

    private boolean shouldStart(Task task) {
        return task.getControllerMode() == Mode.ACTIVE && task.getUnfinishedDependencies() <= 0;
    }

    private boolean shouldStop(Task task) {
        return task.getStopFlag() != StopFlag.NONE;
    }

    private static void assertCanAcceptDependencies(Task task) {
        if (!task.getState().isIdle()) {
            throw new IllegalStateException(String.format("Task %s cannot accept a dependency",
                    task.getName()));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TaskContainer getContainer() {
        return container;
    }

    @Override
    public Mode getMode() {
        return getContainer().getRequiredTask(name).getControllerMode();
    }

    @Override
    public void setMode(Mode mode) {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        Mode currentMode = task.getControllerMode();
        if (currentMode == mode || currentMode == Mode.CANCEL || (mode == Mode.IDLE && currentMode == Mode.ACTIVE)) {
            //no possible movement
            //TODO log
            return;
        }
        task.setControllerMode(mode);
        if (mode == Mode.CANCEL) {
            task.setStopFlag(StopFlag.CANCELLED);
        }

        List<Runnable> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void accept() {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        if (EnumSet.of(State.STARTING,State.UP,State.STOPPING).contains(task.getState())){
            ServerResponse positiveResponse = new ServerResponse(task.getState(), true);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(positiveResponse);
            task.setServerResponses(responses); //probably unnecessary
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Task: " + task.getName() + " State: " + task.getState());
        }

        List<Runnable> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }

    private void doExecute(List<Runnable> tasks) {
        //run in single thread
        for (Runnable task : tasks) {
            task.run();
        }
    }

    @Override
    public void fail() {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Service " + name + "not found"));
        Task task = taskMetadata.getValue();

        if (EnumSet.of(State.STARTING,State.UP, State.STOPPING).contains(task.getState())){
            ServerResponse positiveResponse = new ServerResponse(task.getState(), false);
            List<ServerResponse> responses = task.getServerResponses();
            responses.add(positiveResponse);
            task.setServerResponses(responses); //probably unnecessary
            //maybe assert it was null before
            task.setStopFlag(StopFlag.UNSUCCESSFUL);
        } else {
            throw new IllegalStateException("Got response from the remote entity while not in a state to do so. Service: " + task.getName() + " State: " + task.getState());
        }

        List<Runnable> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Service " + task.getName() + " was remotely updated during the transaction");
        }
    }

    @Override
    public void dependencySucceeded() {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.decUnfinishedDependencies();

        List<Runnable> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        System.out.println("Called dep succeeded on " + name + " and pushed: " + pushed + "with prev version: " + taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }

    }

    @Override
    public void dependencyStopped() {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.setStopFlag(StopFlag.DEPENDENCY_FAILED);

        List<Runnable> tasks = transition(task);
        doExecute(tasks);
        boolean pushed = container.getCache().replaceWithVersion(task.getName(), task, taskMetadata.getVersion());
        if (!pushed) {
            throw new ConcurrentUpdateException("Task " + task.getName() + " was remotely updated during the transaction");
        }
    }


    @Override
    public void dependencyCancelled() {
        assertInTransaction();
        MetadataValue<Task> taskMetadata = container.getCache().getWithMetadata(name);
        assertNotNull(taskMetadata, new TaskNotFoundException("Task " + name + "not found"));
        Task task = taskMetadata.getValue();

        //maybe assert it was null before
        task.setStopFlag(StopFlag.CANCELLED);

        List<Runnable> tasks = transition(task);
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

    private void assertInTransaction() {
        try {
            if (tm.getTransaction() == null) {
                throw new IllegalStateException("Thread not in transaction");
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Unexpected error thrown in TransactionManager", e);
        }
    }

}
