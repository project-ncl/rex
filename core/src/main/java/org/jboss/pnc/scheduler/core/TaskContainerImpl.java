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
import org.jboss.pnc.scheduler.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependant;
import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependency;

@ApplicationScoped
public class TaskContainerImpl extends TaskTargetImpl implements TaskContainer {

    @ConfigProperty(name = "container.name", defaultValue = "undefined")
    String name;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    @Remote("near-tasks")
    RemoteCache<String, Task> tasks;

    private TaskController controller;

    @Inject
    public TaskContainerImpl(TaskController controller) {
        this.controller = controller;
    }

    //FIXME implement
    public void shutdown() {
        throw new UnsupportedOperationException("Currently not implemented!");
    }

    public String getName() {
        return name;
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
        };
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
            throw new IllegalStateException("Part of transaction was committed and part rollback. Data corruption possible.", e);
        } catch (SystemException | NotSupportedException | HeuristicRollbackException e) {
            throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown while committing transactions", e);
        }
    }

    private void installInternal(BatchTaskInstallerImpl taskBuilder) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        boolean joined = joinOrBeginTransaction();
        try {
            Set<String> installed = taskBuilder.getInstalledTasks();

            for (TaskBuilderImpl declaration : taskBuilder.getTaskDeclarations()) {

                Task newTask = declaration.toPartiallyFilledTask();
                for (String dependant : declaration.getDependants()) {
                    if (installed.contains(dependant)) {
                        //get existing dependant
                        MetadataValue<Task> dependantTaskMetadata = getWithMetadata(dependant);
                        if (dependantTaskMetadata == null) {
                            throw new TaskNotFoundException("Task " + dependant + " was not found while installing Batch");
                        }
                        Task dependantTask = dependantTaskMetadata.getValue();

                        //add new task as dependency to existing dependant
                        newDependency(dependantTask, newTask);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependant, dependantTask, dependantTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Task " + dependant + " was remotely updated during the transaction");
                        }
                    }
                    //dependants are already initialized in SBImpl::toPartiallyFilledTask
                }

                int unfinishedDependencies = 0;
                for (String dependency : declaration.getDependencies()) {
                    if (installed.contains(dependency)) {
                        //get existing dependency
                        MetadataValue<Task> dependencyTaskMetadata = getWithMetadata(dependency);
                        Task dependencyTask = dependencyTaskMetadata.getValue();
                        if (dependencyTaskMetadata == null) {
                            throw new TaskNotFoundException("Task " + dependency + " was not found while installing Batch");
                        }

                        //add new Task as dependant to existing dependency
                        newDependant(dependencyTask, newTask);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependency, dependencyTask, dependencyTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Task " + dependency + " was remotely updated during the transaction");
                        }
                        if (dependencyTask.getState().isFinal()) {
                            continue; //skip, unfinishedDep inc not needed
                        }
                    }
                    unfinishedDependencies++;
                }
                newTask.setUnfinishedDependencies(unfinishedDependencies);

                Task previousValue = getCache().withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(newTask.getName(), newTask);
                if (previousValue != null) {
                    throw new InvalidTaskDeclarationException("Task " + newTask.getName() + " already exists.");
                }

            }
            //check for cycles using DFS
            hasCycle(taskBuilder.getTaskDeclarations().stream().map(TaskBuilderImpl::getName).collect(Collectors.toSet()));
            //All services should be saved, now start up ones declared ACTIVE
            for (TaskBuilderImpl taskDeclaration : taskBuilder.getTaskDeclarations()) {
                String name = taskDeclaration.getName();
                if (taskDeclaration.getInitialMode() == Mode.ACTIVE) {
                    controller.setMode(name, Mode.ACTIVE);
                }
            }
            if (!joined) getTransactionManager().commit();
        } catch (RuntimeException e) {
            //rollback on failure
            if (!joined) getTransactionManager().rollback();
            throw e;
        }
    }

    @Override
    public List<Task> getTask(boolean waiting, boolean running, boolean finished) {
        if (!waiting && !running && !finished) return Collections.emptyList();

        List<State> states = new ArrayList<>();
        if (waiting) {
            states.addAll(EnumSet.of(State.NEW, State.WAITING));
        }
        if (running) {
            states.addAll(EnumSet.of(State.UP, State.STARTING, State.STOPPING));
        }
        if (finished) {
            states.addAll(EnumSet.of(State.STOPPED, State.SUCCESSFUL, State.FAILED, State.START_FAILED, State.START_FAILED));
        }
        QueryFactory factory = Search.getQueryFactory(tasks);
        Query query = factory.from(Task.class)
                .having("state").containsAny(states)
                .build();

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
        if (getTransactionManager().getTransaction() == null){
            getTransactionManager().begin();
            return false;
        }
        return true;
    }

    private boolean hasCycle(Set<String> taskIds) {
        Set<String> notVisited = new HashSet<String>(taskIds);
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        while (notVisited.size() > 0) {
            String current = notVisited.iterator().next();
            if (dfs(current, notVisited, visiting, visited)) {
                throw new CircularDependencyException("Cycle has been found on Task " + current);
            }
        }
        return false;
    }

    private boolean dfs(String current, Set<String> notVisited, Set<String> visiting, Set<String> visited) {
        move(current, notVisited, visiting);
        Task currentTask = getTask(current);
        for (String dependency : currentTask.getDependencies()) {
            //attached dependencies are not in the builder declaration, therefore if discovered, they have to be add as notVisited
            if (!notVisited.contains(dependency) && !visiting.contains(dependency) && !visited.contains(dependency)){
                notVisited.add(dependency);
            }
            //already explored, continue
            if (visited.contains(dependency)) {
                continue;
            }
            //visiting again, cycle found
            if (visiting.contains(dependency)) {
                return true;
            }
            //recursive call
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
}
