package org.jboss.pnc.scheduler.core;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.*;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.msc.service.ServiceName;
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
import javax.transaction.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependant;
import static org.jboss.pnc.scheduler.core.TaskControllerImpl.newDependency;

@ApplicationScoped
public class TaskContainerImpl extends TaskTargetImpl implements TaskContainer {

    @ConfigProperty(name = "container.name", defaultValue = "undefined")
    String name;

    @ConfigProperty(name = "scheduler.baseUrl", defaultValue = "http://localhost:8080")
    String baseUrl;

    private RemoteCache<ServiceName, Task> tasks;

    //CDI
    @Deprecated
    public TaskContainerImpl() {
    }
    @Inject
    public TaskContainerImpl(RemoteCacheManager cacheManager, TransactionManager transactionManager) {
        tasks = cacheManager.getCache("near-tasks", TransactionMode.NON_DURABLE_XA, transactionManager);
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

    public TaskController getTaskController(ServiceName task) {
        return getTaskControllerInternal(task);
    }

    public TaskControllerImpl getTaskControllerInternal(ServiceName task) {
        return new TaskControllerImpl(task, this);
    }

    public Task getTask(ServiceName task) {
        return tasks.get(task);
    }

    public TaskController getRequiredTaskController(ServiceName task) throws TaskNotFoundException {
        Task s = getCache().get(task);
        if (s == null) {
            throw new TaskNotFoundException("Task with name " + task.getCanonicalName() + " was not found");
        };
        return getTaskController(task);
    }

    public Task getRequiredTask(ServiceName task) throws TaskNotFoundException {
        Task s = getCache().get(task);
        if (s == null) {
            throw new TaskNotFoundException("Task with name " + task.getCanonicalName() + " was not found");
        };
        return s;
    }

    public Collection<ServiceName> getTaskIds() {
        return tasks.keySet();
    }

    public TransactionManager getTransactionManager() {
        return tasks.getTransactionManager();
    }

    public RemoteCache<ServiceName, Task> getCache() {
        return tasks;
    }

    public MetadataValue<Task> getWithMetadata(ServiceName name) {
        return tasks.getWithMetadata(name);
    }

    @Override
    //TODO check for Circle
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
            Set<ServiceName> installed = taskBuilder.getInstalledTasks();

            for (TaskBuilderImpl declaration : taskBuilder.getTaskDeclarations()) {

                Task newTask = declaration.toPartiallyFilledTask();
                for (ServiceName dependant : declaration.getDependants()) {
                    if (installed.contains(dependant)) {
                        //get existing dependant
                        MetadataValue<Task> dependantTaskMetadata = getWithMetadata(dependant);
                        Task dependantTask = dependantTaskMetadata.getValue();
                        if (dependantTaskMetadata == null) {
                            throw new TaskNotFoundException("Task " + dependant.getCanonicalName() + " was not found while installing Batch");
                        }

                        //add new task as dependency to existing dependant
                        newDependency(dependantTask, newTask);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependant, dependantTask, dependantTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Task " + dependant.getCanonicalName() + " was remotely updated during the transaction");
                        }
                    }
                    //dependants are already initialized in SBImpl::toPartiallyFilledTask
                }

                int unfinishedDependencies = 0;
                for (ServiceName dependency : declaration.getDependencies()) {
                    if (installed.contains(dependency)) {
                        //get existing dependency
                        MetadataValue<Task> dependencyTaskMetadata = getWithMetadata(dependency);
                        Task dependencyTask = dependencyTaskMetadata.getValue();
                        if (dependencyTaskMetadata == null) {
                            throw new TaskNotFoundException("Task " + dependency.getCanonicalName() + " was not found while installing Batch");
                        }

                        //add new Task as dependant to existing dependency
                        newDependant(dependencyTask, newTask);

                        //update the dependency
                        if (!getCache().replaceWithVersion(dependency, dependencyTask, dependencyTaskMetadata.getVersion())) {
                            throw new ConcurrentUpdateException("Task " + dependency.getCanonicalName() + " was remotely updated during the transaction");
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
                    throw new InvalidTaskDeclarationException("Task " + newTask.getName().getCanonicalName() + " already exists.");
                }

            }
            //check for cycles using DFS
            hasCycle(taskBuilder.getTaskDeclarations().stream().map(TaskBuilderImpl::getName).collect(Collectors.toSet()));
            //All services should be saved, now start up ones declared ACTIVE
            for (TaskBuilderImpl taskDeclaration : taskBuilder.getTaskDeclarations()) {
                ServiceName name = taskDeclaration.getName();
                if (taskDeclaration.getInitialMode() == Mode.ACTIVE) {
                    getTaskController(name).setMode(Mode.ACTIVE);
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

    private boolean hasCycle(Set<ServiceName> taskIds) {
        Set<ServiceName> notVisited = new HashSet<>(taskIds);
        Set<ServiceName> visiting = new HashSet<>();
        Set<ServiceName> visited = new HashSet<>();

        while (notVisited.size() > 0) {
            ServiceName current = notVisited.iterator().next();
            if (dfs(current, notVisited, visiting, visited)) {
                throw new CircularDependencyException("Cycle has been found on Task " + current.getCanonicalName());
            }
        }
        return false;
    }

    private boolean dfs(ServiceName current, Set<ServiceName> notVisited, Set<ServiceName> visiting, Set<ServiceName> visited) {
        move(current, notVisited, visiting);
        Task currentTask = getTask(current);
        for (ServiceName dependency : currentTask.getDependencies()) {
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

    private void move(ServiceName name, Set<ServiceName> sourceSet, Set<ServiceName> destinationSet) {
        sourceSet.remove(name);
        destinationSet.add(name);
    }
}
