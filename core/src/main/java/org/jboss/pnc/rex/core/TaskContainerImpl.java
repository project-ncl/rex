/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core;

import io.quarkus.infinispan.client.Remote;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.exceptions.BadRequestException;
import org.jboss.pnc.rex.common.exceptions.CircularDependencyException;
import org.jboss.pnc.rex.common.exceptions.ConstraintConflictException;
import org.jboss.pnc.rex.common.exceptions.TaskConflictException;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.rex.common.exceptions.TaskMissingException;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.core.api.TaskTarget;
import org.jboss.pnc.rex.core.jobs.ControllerJob;
import org.jboss.pnc.rex.core.jobs.PokeQueueJob;
import org.jboss.pnc.rex.core.mapper.InitialTaskMapper;
import org.jboss.pnc.rex.core.model.Edge;
import org.jboss.pnc.rex.core.model.InitialTask;
import org.jboss.pnc.rex.core.model.TaskGraph;
import org.jboss.pnc.rex.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
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

import static javax.transaction.Transactional.TxType.MANDATORY;

@Slf4j
@ApplicationScoped
public class TaskContainerImpl implements TaskContainer, TaskTarget {

    @ConfigProperty(name = "scheduler.name", defaultValue = "undefined")
    String deploymentName;

    @ConfigProperty(name = "scheduler.baseUrl")
    String baseUrl;

    @Remote("rex-tasks")
    RemoteCache<String, Task> tasks;

    @Remote("rex-constraints")
    RemoteCache<String, String> constraints;

    private final TaskController controller;

    private final InitialTaskMapper initialMapper;

    private final Event<ControllerJob> jobEvent;

    @Inject
    public TaskContainerImpl(TaskController controller, InitialTaskMapper initialMapper, Event<ControllerJob> jobEvent) {
        this.controller = controller;
        this.initialMapper = initialMapper;
        this.jobEvent = jobEvent;
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

    public Task getRequiredTask(String task) throws TaskMissingException {
        Task s = getCache().get(task);
        if (s == null) {
            throw new TaskMissingException("Task with name " + task + " was not found", task);
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

    public RemoteCache<String, String> getConstraintCache() {
        return constraints;
    }

    public VersionedValue<Task> getWithMetadata(String name) {
        return tasks.getWithMetadata(name);
    }

    public VersionedValue<Task> getRequiredTaskWithMetadata(String name) {
        VersionedValue<Task> meta = tasks.getWithMetadata(name);
        if (meta == null) {
            log.info("ERROR: couldn't find task {}", name);
            throw new TaskMissingException("Task with name " + name + " was not found", name);
        }
        return meta;
    }

    @Override
    public List<Task> getTasks(boolean waiting, boolean running, boolean finished) {
        if (!waiting && !running && !finished)
            return Collections.emptyList();

        List<State> states = new ArrayList<>();
        if (waiting) {
            states.addAll(EnumSet.of(State.NEW, State.WAITING, State.ENQUEUED));
        }
        if (running) {
            states.addAll(EnumSet.of(State.UP, State.STARTING, State.STOPPING));
        }
        if (finished) {
            states.addAll(
                    EnumSet.of(State.STOPPED, State.SUCCESSFUL, State.FAILED, State.START_FAILED, State.STOP_FAILED));
        }
        QueryFactory factory = Search.getQueryFactory(tasks);
        // reduce to 'NEW','WAITING'.... format
        String filter = states.stream()
                .map(state -> "'" + state.toString() + "'")
                .reduce((first, second) -> first + ',' + second)
                .get();
        Query<Task> query = factory.create("FROM rex_model.Task WHERE state IN (" + filter + ")");

        return query.execute().list();
    }

    @Override
    public List<Task> getEnqueuedTasks(long limit) {
        QueryFactory factory = Search.getQueryFactory(tasks);
        Query<Task> query = factory.create("FROM rex_model.Task WHERE state = '" + State.ENQUEUED + "'");
        return query.maxResults((int) limit).execute().list();
    }

    @Override
    public List<Task> getTasksByCorrelationID(String correlationID) {
        QueryFactory factory = Search.getQueryFactory(tasks);
        Query<Task> taskQuery = factory.create("FROM rex_model.Task WHERE correlationID = :correlationID");
        taskQuery.setParameter("correlationID", correlationID);
        return taskQuery.execute().list();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private void hasCycle(Set<String> taskIds) throws CircularDependencyException {
        Set<String> notVisited = new HashSet<>(taskIds);
        List<String> visiting = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        while (notVisited.size() > 0) {
            String current = notVisited.iterator().next();
            if (dfs(current, notVisited, visiting, visited)) {
                throw new CircularDependencyException("Cycle has been found on Task " + current + " with loop: " + formatCycle(visiting, current));
            }
        }
    }

    private String formatCycle(List<String> loopPath, String current) {
        StringBuilder loop = new StringBuilder();
        for (String node : loopPath) {
            loop.append(node).append("->");
        }
        return loop.append(current).toString();
    }

    private boolean dfs(String current, Set<String> notVisited, List<String> visiting, Set<String> visited) {
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

    private void move(String name, Collection<String> sourceSet, Collection<String> destinationSet) {
        sourceSet.remove(name);
        destinationSet.add(name);
    }

    @Transactional(MANDATORY)
    public Set<Task> install(TaskGraph taskGraph) {
        log.info("Install requested: " + taskGraph.toString());
        Set<Edge> edges = taskGraph.getEdges();
        Map<String, InitialTask> vertices = taskGraph.getVertices();
        Map<String, Task> taskCache = new HashMap<>();

        // handle edge by edge
        for (Edge edge : edges) {
            assertEdgeValidity(edge);

            String dependant = edge.getSource();
            Task dependantTask = addToLocalCache(dependant, taskCache, vertices);

            assertDependantCanHaveDependency(dependantTask);

            String dependency = edge.getTarget();
            Task dependencyTask = addToLocalCache(dependency, taskCache, vertices);

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

        // poke the queue to start new ENQUEUED tasks if there is room (NOTE: queue is poked after current transaction
        // succeeds)
        jobEvent.fire(new PokeQueueJob());
        return newTasks;
    }

    private void assertEdgeValidity(Edge edge) {
        if (edge.getSource().equals(edge.getTarget())) {
            throw new BadRequestException("Invalid edge. Task " + edge.getSource() + " cannot depend on itself.");
        }
    }

    private Set<Task> storeTheTasks(Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
        Set<Task> toReturn = new HashSet<>();
        for (Map.Entry<String, Task> entry : taskCache.entrySet()) {
            Task task = entry.getValue();
            if (isNewTask(entry.getKey(), vertices)) {
                Task previousValue = getCache().withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(task.getName(), task);
                if (previousValue != null) {
                    throw new TaskConflictException(
                            "Task " + task.getName() + " declared as new in vertices already exists.", previousValue.getName());
                }

                handleOptionalConstraint(task);

                // return only new tasks
                toReturn.add(task);
            } else {
                // we have to get the previous version
                VersionedValue<Task> versioned = getWithMetadata(entry.getKey());
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

    private void handleOptionalConstraint(Task task) throws ConstraintConflictException {
        String constraint = task.getConstraint();
        if (constraint != null) {
            String previousHolder = constraints.putIfAbsent(constraint, task.getName());
            if (previousHolder != null) {
                throw new ConstraintConflictException("Task " + task.getName() + " with constraint '" + task.getConstraint() +"' in conflict. Conflicting Task: " + previousHolder, constraint);
            }
        }
    }

    private void addTasksWithoutEdgesToCache(Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
        Set<String> newTasksWithoutEdges = new HashSet<>(vertices.keySet());
        newTasksWithoutEdges.removeAll(taskCache.keySet());
        for (String simpleTask : newTasksWithoutEdges) {
            addToLocalCache(simpleTask, taskCache, vertices);
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

    private Task addToLocalCache(String name, Map<String, Task> taskCache, Map<String, InitialTask> vertices) {
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
                throw new BadRequestException(
                        "Either task with the identifier " + name
                                + " is not present or data for a new task is not declared in vertices");
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
            throw new BadRequestException(
                    "Existing task " + dependant.getName() + " is in " + dependant.getState()
                            + " state and cannot have more dependencies.");
        }
    }

    private boolean isNewTask(String task, Map<String, InitialTask> vertices) {
        return vertices.containsKey(task);
    }
}
