package org.jboss.pnc.scheduler.core;

import io.quarkus.infinispan.client.Remote;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.exceptions.CircularDependencyException;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.core.api.TaskController;
import org.jboss.pnc.scheduler.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.common.exceptions.InvalidTaskDeclarationException;
import org.jboss.pnc.scheduler.common.exceptions.TaskNotFoundException;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.mapper.InitialTaskMapper;
import org.jboss.pnc.scheduler.core.model.Edge;
import org.jboss.pnc.scheduler.core.model.InitialTask;
import org.jboss.pnc.scheduler.core.model.TaskGraph;
import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependant;
import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependency;

@ApplicationScoped
public class TaskContainerImpl extends TaskTargetImpl implements TaskContainer {

    @ConfigProperty(name = "container.name", defaultValue = "undefined")
    String deploymentName;

    @ConfigProperty(name = "scheduler.baseUrl")
    String baseUrl;

    @Remote("near-tasks")
    RemoteCache<String, Task> tasks;

    private final TaskController controller;

    private final InitialTaskMapper initialMapper;

    @Inject
    public TaskContainerImpl(TaskController controller, InitialTaskMapper initialMapper) {
        this.controller = controller;
        this.initialMapper = initialMapper;
    }

    // FIXME implement
    public void shutdown() {
        throw new UnsupportedOperationException("Currently not implemented!");
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public boolean isShutdown() {
        return false;
    }

    public Task getTask(String task) {
        return tasks.get(task);
    }

    public Task getRequiredTask(String task) throws TaskNotFoundException {
        Task s = getCache().get(task);
        if (s == null) {
            throw new TaskNotFoundException("Task with name " + task + " was not found");
        }
        return s;
    }

    public Collection<String> getTaskIds() {
        return tasks.keySet();
    }

    public TransactionManager getTransactionManager() {
        return tasks.getTransactionManager();
    }

    public RemoteCache<String, Task> getCache() {
        return tasks;
    }

    public MetadataValue<Task> getWithMetadata(String name) {
        return tasks.getWithMetadata(name);
    }

    @Override
    protected void install(BatchTaskInstallerImpl taskBuilder) {
        try {
            installInternal(taskBuilder);
        } catch (RollbackException e) {
            throw new IllegalStateException("Installation rolled back.", e);
        } catch (HeuristicMixedException e) {
            throw new IllegalStateException(
                    "Part of transaction was committed and part rollback. Data corruption possible.",
                    e);
        } catch (SystemException | NotSupportedException | HeuristicRollbackException e) {
            throw new IllegalStateException(
                    "Cannot start Transaction, unexpected error was thrown while committing transactions",
                    e);
        }
    }

    private void installInternal(BatchTaskInstallerImpl taskBuilder) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        boolean joined = joinOrBeginTransaction();
        try {
            Set<String> installed = taskBuilder.getInstalledTasks();

            for (TaskBuilderImpl declaration : taskBuilder.getTaskDeclarations()) {

                Task newTask = declaration.toPartiallyFilledTask();
                for (String dependant : declaration.getDependants()) {
                    if (installed.contains(dependant)) {
                        // get existing dependant
                        MetadataValue<Task> dependantTaskMetadata = getWithMetadata(dependant);
                        if (dependantTaskMetadata == null) {
                            throw new TaskNotFoundException(
                                    "Task " + dependant + " was not found while installing Batch");
                        }
                        Task dependantTask = dependantTaskMetadata.getValue();

                        // add new task as dependency to existing dependant
                        newDependency(dependantTask, newTask);

                        // update the dependency
                        if (!getCache()
                                .replaceWithVersion(dependant, dependantTask, dependantTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException(
                                    "Task " + dependant + " was remotely updated during the transaction");
                        }
                    }
                    // dependants are already initialized in SBImpl::toPartiallyFilledTask
                }

                int unfinishedDependencies = 0;
                for (String dependency : declaration.getDependencies()) {
                    if (installed.contains(dependency)) {
                        // get existing dependency
                        MetadataValue<Task> dependencyTaskMetadata = getWithMetadata(dependency);
                        if (dependencyTaskMetadata == null) {
                            throw new TaskNotFoundException(
                                    "Task " + dependency + " was not found while installing Batch");
                        }

                        // add new Task as dependant to existing dependency
                        Task dependencyTask = dependencyTaskMetadata.getValue();
                        newDependant(dependencyTask, newTask);

                        // update the dependency
                        if (!getCache()
                                .replaceWithVersion(dependency, dependencyTask, dependencyTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException(
                                    "Task " + dependency + " was remotely updated during the transaction");
                        }
                        if (dependencyTask.getState().isFinal()) {
                            continue; // skip, unfinishedDep inc not needed
                        }
                    }
                    unfinishedDependencies++;
                }
                newTask.setUnfinishedDependencies(unfinishedDependencies);

                Task previousValue = getCache().withFlags(Flag.FORCE_RETURN_VALUE)
                        .putIfAbsent(newTask.getName(), newTask);
                if (previousValue != null) {
                    throw new InvalidTaskDeclarationException("Task " + newTask.getName() + " already exists.");
                }

            }
            // check for cycles using DFS
            hasCycle(
                    taskBuilder.getTaskDeclarations()
                            .stream()
                            .map(TaskBuilderImpl::getName)
                            .collect(Collectors.toSet()));
            // All services should be saved, now start up ones declared ACTIVE
            for (TaskBuilderImpl taskDeclaration : taskBuilder.getTaskDeclarations()) {
                String name = taskDeclaration.getName();
                if (taskDeclaration.getInitialMode() == Mode.ACTIVE) {
                    controller.setMode(name, Mode.ACTIVE);
                }
            }
            if (!joined)
                getTransactionManager().commit();
        } catch (RuntimeException e) {
            // rollback on failure
            if (!joined)
                getTransactionManager().rollback();
            throw e;
        }
    }

    @Override
    public List<Task> getTask(boolean waiting, boolean running, boolean finished) {
        if (!waiting && !running && !finished)
            return Collections.emptyList();

        List<State> states = new ArrayList<>();
        if (waiting) {
            states.addAll(EnumSet.of(State.NEW, State.WAITING));
        }
        if (running) {
            states.addAll(EnumSet.of(State.UP, State.STARTING, State.STOPPING));
        }
        if (finished) {
            states.addAll(
                    EnumSet.of(State.STOPPED, State.SUCCESSFUL, State.FAILED, State.START_FAILED, State.START_FAILED));
        }
        QueryFactory factory = Search.getQueryFactory(tasks);
        Query query = factory.from(Task.class).having("state").containsAny(states).build();

        return query.list();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     *
     * @return returns true if joined
     */
    private boolean joinOrBeginTransaction() throws SystemException, NotSupportedException {
        if (getTransactionManager().getTransaction() == null) {
            getTransactionManager().begin();
            return false;
        }
        return true;
    }

    private void hasCycle(Set<String> taskIds) throws CircularDependencyException {
        Set<String> notVisited = new HashSet<String>(taskIds);
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        while (notVisited.size() > 0) {
            String current = notVisited.iterator().next();
            if (dfs(current, notVisited, visiting, visited)) {
                throw new CircularDependencyException("Cycle has been found on Task " + current);
            }
        }
    }

    private boolean dfs(String current, Set<String> notVisited, Set<String> visiting, Set<String> visited) {
        move(current, notVisited, visiting);
        Task currentTask = getTask(current);
        for (String dependency : currentTask.getDependencies()) {
            // attached dependencies are not in the builder declaration, therefore if discovered, they have to be add as
            // notVisited
            if (!notVisited.contains(dependency) && !visiting.contains(dependency) && !visited.contains(dependency)) {
                notVisited.add(dependency);
            }
            // already explored, continue
            if (visited.contains(dependency)) {
                continue;
            }
            // visiting again, cycle found
            if (visiting.contains(dependency)) {
                return true;
            }
            // recursive call
            if (dfs(dependency, notVisited, visiting, visited)) {
                return true;
            }
        }
        move(current, visiting, visited);
        return false;
    }

    private void move(String name, Set<String> sourceSet, Set<String> destinationSet) {
        sourceSet.remove(name);
        destinationSet.add(name);
    }

    // ###################
    // #### V2 ####
    // ###################

    @Override
    @Transactional(MANDATORY)
    public Set<Task> install(TaskGraph taskGraph) {
        Set<Edge> edges = taskGraph.getEdges();
        Map<String, InitialTask> vertices = taskGraph.getVertices();
        Map<String, Task> taskCache = new HashMap<>();

        // handle edge by edge
        for (Edge edge : edges) {
            String dependant = edge.getSource();
            Task dependantTask = addToCache(dependant, taskCache, vertices);

            assertDependantCanHaveDependency(dependantTask);

            String dependency = edge.getTarget();
            Task dependencyTask = addToCache(dependency, taskCache, vertices);

            updateTasks(dependencyTask, dependantTask);
        }

        // add simple new tasks to cache that have no dependencies nor dependants
        addTasksWithoutEdgesToCache(taskCache, vertices);

        Set<Task> newTasks = storeTheTasks(taskCache, vertices);

        hasCycle(taskCache.keySet());

        // start the tasks
        newTasks.forEach(task -> {
            if (task.getControllerMode() == Mode.ACTIVE)
                controller.setMode(task.getName(), Mode.ACTIVE);
        });
        return newTasks;
    }

    private Set<Task> storeTheTasks(Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
        Set<Task> toReturn = new HashSet<>();
        for (Map.Entry<String, Task> entry : taskCache.entrySet()) {
            Task task = entry.getValue();
            if (isNewTask(entry.getKey(), vertices)) {
                Task previousValue = getCache().withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(task.getName(), task);
                if (previousValue != null) {
                    throw new IllegalArgumentException(
                            "Task " + task.getName() + " declared as new in vertices already exists.");
                }

                // return only new tasks
                toReturn.add(task);
            } else {
                // we have to get the previous version
                MetadataValue<Task> versioned = getCache().getWithMetadata(entry.getKey());
                // could be optimized by using #putAll method of remote cache (only 'else' part could be; the 'then'
                // part required FORCE_RETURN_VALUE flag which is not available in putAll)
                boolean success = getCache().replaceWithVersion(entry.getKey(), versioned.getValue(), versioned.getVersion());
                if (!success) {
                    throw new ConcurrentUpdateException(
                            "Task " + versioned.getValue() + " was remotely updated during the transaction");
                }
            }
        }
        return toReturn;
    }

    private void addTasksWithoutEdgesToCache(Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
        Set<String> newTasksWithoutEdges = new HashSet<>(vertices.keySet());
        newTasksWithoutEdges.removeAll(taskCache.keySet());
        for (String simpleTask : newTasksWithoutEdges) {
            addToCache(simpleTask, taskCache, vertices);
        }
    }

    private void updateTasks(Task dependencyTask, Task dependantTask) {
        addDependency(dependantTask, dependencyTask);
        addDependant(dependencyTask, dependantTask);
    }

    private void addDependency(Task dependant, Task dependency) {
        if (dependant.getDependencies().contains(dependency.getName())) {
            return;
        }

        dependant.getDependencies().add(dependency.getName());
        // increase amount of unfinished dependencies if the dependency hasn't finished
        if (!dependency.getState().isFinal()) {
            dependant.incUnfinishedDependencies();
        }
    }

    private void addDependant(Task dependency, Task dependant) {
        if (dependency.getDependants().contains(dependant.getName())) {
            return;
        }

        dependency.getDependants().add(dependant.getName());
    }

    private Task addToCache(String name, Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
        if (taskCache.containsKey(name)) {
            return taskCache.get(name);
        }

        Task task;
        if (isNewTask(name, vertices)) {
            // task data for new tasks should be in the vertices
            task = initialMapper.fromInitialTask(vertices.get(name));

            // workaround for lombok builder's immutable collections
            task.setDependants(new HashSet<>(task.getDependants()));
            task.setDependencies(new HashSet<>(task.getDependencies()));
        } else {
            // task data for existing task should be retrieved from DB
            task = getTask(name);
            if (task == null) {
                throw new IllegalArgumentException(
                        "Either existing task" + name
                                + " has incorrect identifier or data for a new task is not declared in vertices");
            }
        }
        taskCache.put(name, task);
        return task;
    }

    /**
     * DEPENDANT -> DEPENDENCY
     * 
     * Dependant can have a dependency if it's currently in an IDLE state (hasn't finished nor started)
     */
    private void assertDependantCanHaveDependency(Task dependant) {
        if (!dependant.getState().isIdle()) {
            throw new IllegalArgumentException(
                    "Existing task " + dependant.getName() + " is in " + dependant.getState()
                            + " state and cannot have more dependencies.");
        }
    }

    private boolean isNewTask(String task, Map<String, InitialTask> vertices) {
        return vertices.containsKey(task);
    }
}
