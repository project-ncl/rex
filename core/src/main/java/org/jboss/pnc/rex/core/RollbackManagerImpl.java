/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
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

import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import com.google.common.graph.Traverser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.common.enums.StopFlag;
import org.jboss.pnc.rex.core.api.RollbackManager;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.model.Task;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.filter;
import static java.util.stream.Collectors.toMap;

@Slf4j
@ApplicationScoped
public class RollbackManagerImpl implements RollbackManager {

    private final static Set<State> ROLLBACK_DENIED = Set.of(State.STOP_REQUESTED, State.STOPPING, State.STOP_FAILED,
            State.TO_ROLLBACK, State.ROLLBACK_REQUESTED, State.ROLLINGBACK, State.ROLLEDBACK, State.ROLLBACK_FAILED);
    private final static Set<StopFlag> PROBLEMATIC_FLAGS = Set.of(StopFlag.DEPENDENCY_FAILED, StopFlag.DEPENDENCY_NOTIFY_FAILED);

    private final TaskController controller;

    private final TaskRegistry registry;

    public RollbackManagerImpl(TaskController controller, TaskRegistry registry) {
        this.controller = controller;
        this.registry = registry;
    }

    @Override
    @Transactional
    public void rollbackFromMilestone(String name) {
        Graph<Task> taskGraph = registry.getTaskGraph(Set.of(name));
        Task milestone = taskGraph.nodes().stream()
                .filter(task -> task.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Task " + name + " is missing in Task graph."));

        assertMilestoneState(milestone);

        Set<Task> candidates = Sets.newHashSet(Traverser.forGraph(Graphs.transpose(taskGraph)).breadthFirst(milestone));

        //filter out ignored
        candidates = candidates.stream()
                .filter(task -> !ROLLBACK_DENIED.contains(task.getState()))
                .filter(task -> task.getStopFlag() != StopFlag.CANCELLED)
                .collect(Collectors.toSet());


        removeProblematicTasksFromCandidates(candidates, name, taskGraph);

        involveAdjacentTasksInTransaction(taskGraph, candidates);

        List<Task> sortedCandidates = topologicalSort(Graphs.inducedSubgraph(taskGraph, candidates));

        Set<Task> immediateRollback = new HashSet<>();
        for (Task candidate : sortedCandidates) {
            int futureRunningDependencies = 0;
            int rollBackingDependants = 0;
            for (Task dependant : taskGraph.predecessors(candidate)) {
                if (immediateRollback.contains(dependant)) {
                    continue;
                }

                if (candidates.contains(dependant)) {
                    // in candidates
                    rollBackingDependants += switch (dependant.getState().getGroup()) {
                        // will get transferred into ROLLEDBACK
                        case IDLE, QUEUED -> 0;

                        case RUNNING, FINAL, ROLLBACK_TODO -> 1;

                        // all filtered out from candidates by ROLLBACK_DENIED
                        case ROLLBACK -> 0;
                    };
                } else {
                    // not in candidates (can be Task in ROLLBACK_DENIED)
                    rollBackingDependants += switch (dependant.getState().getGroup()) {
                        // should be filtered out
                        case IDLE, QUEUED, RUNNING, FINAL, ROLLBACK_TODO -> 0;

                        // handle case when this rollback attaches to a different in-progress rollback branch
                        case ROLLBACK -> switch (candidate.getState()) {
                                // parent task from different Rollback branch has not yet rolledback (increase count)
                                case TO_ROLLBACK, ROLLBACK_REQUESTED, ROLLINGBACK -> {
                                    int dependantUnfinishedDependencies = dependant.getUnfinishedDependencies();
                                    int dependantRollbackDependants = dependant.getRollbackMeta().getUnrestoredDependants();

                                    // dependant will have a new rollingback dependency
                                    controller.primeForRollback(dependant.getName(), dependantRollbackDependants, dependantUnfinishedDependencies + 1);
                                    yield 1;
                                }
                                case ROLLBACK_FAILED, ROLLEDBACK -> 0;
                                default -> 0;
                        };
                    };
                }
            }

            // no action even if there's a RemoteRollback defined
            Set<State> fastForwardStates = Set.of(State.NEW, State.WAITING, State.STOPPED);
            if ((candidate.getRemoteRollback() == null ||
                    (fastForwardStates.contains(candidate.getState())))
                    && rollBackingDependants == 0) {
                immediateRollback.add(candidate);
            }

            for (Task dependency : taskGraph.successors(candidate)) {
                if (candidates.contains(dependency)) {
                    futureRunningDependencies++;
                } else {
                    futureRunningDependencies += switch (dependency.getState().getGroup()) {
                        case IDLE, QUEUED, RUNNING, ROLLBACK, ROLLBACK_TODO -> 1;
                        case FINAL -> 0;
                    };
                }
            }
            log.debug("ROLLBACK {}: TASK '{}' priming with dependants={} and dependencies={}", milestone.getName(), candidate.getName(), rollBackingDependants, futureRunningDependencies);
            controller.primeForRollback(candidate.getName(), rollBackingDependants, futureRunningDependencies);
        }

        controller.startRollbackProcess(name);
    }

    private void assertMilestoneState(Task milestone) {
        Set<State> allowedBeginningStates = Set.of(State.SUCCESSFUL, State.ROLLBACK_TRIGGERED);
        if (!allowedBeginningStates.contains(milestone.getState())) {
            throw new IllegalStateException("Task " + milestone.getName() + " can't be milestone with state " + milestone.getState());
        }
    }

    /**
     * This method forces all candidates' adjacent tasks, to be in the Transaction.
     *
     * Motivation: There are a lot of conditions involving adjacent tasks that set counters for candidates and these
     * counters need to be consistent.
     *
     * This prevents race conditions if these adjacent Tasks are modified in a parallel transaction.
     *
     * @param taskGraph graph of the milestone
     * @param candidates tasks directly involved in rollback
     */
    private void involveAdjacentTasksInTransaction(Graph<Task> taskGraph, Set<Task> candidates) {
        Set<Task> doneTasks = new HashSet<>(candidates);
        for (Task candidate : candidates) {
            for (Task task : taskGraph.adjacentNodes(candidate)) {
                if (doneTasks.contains(task)) {
                    continue;
                }

                doneTasks.add(task);
                controller.involveInTransaction(task.getName());
            }
        }

    }

    // todo javadoc
    //handle case when DEPENDENCY_FAILED/NOTIFY_FAILED case may have cause outside candidates (we cannot rollback these tasks)
    private static void removeProblematicTasksFromCandidates(Set<Task> candidates, String milestone, Graph<Task> taskGraph) {
        Set<Task> problematicTasks = candidates.stream()
                .filter(task -> PROBLEMATIC_FLAGS.contains(task.getStopFlag()))
                .collect(Collectors.toSet());


        if (!problematicTasks.isEmpty()) {
            for (Task problematicTask : problematicTasks) {
                var deps = Traverser.forGraph(taskGraph).breadthFirst(problematicTask);
                var possibleFailOrigins = Sets.newHashSet(filter(deps, task -> Set.of(State.FAILED, State.START_FAILED).contains(task.getState())));

                if (problematicTask.getStopFlag() == StopFlag.DEPENDENCY_NOTIFY_FAILED) {
                    // for DEPENDENCY_NOTIFY_FAILED, stop cause can be a SUCCESS task that can be found only with stoppedCause
                    Optional<Task> problematicTaskStopCause = taskGraph.nodes()
                            .stream()
                            .filter(task -> task.getName().equals(problematicTask.getStoppedCause()))
                            .findFirst();
                    problematicTaskStopCause.ifPresent(possibleFailOrigins::add);
                }

                if (candidates.containsAll(possibleFailOrigins)) {
                    continue;
                }

                Set<Task> outerOrigins = Sets.difference(possibleFailOrigins, candidates);
                log.debug("ROLLBACK {}: Can't rollback '{}' because failure origin is not dependant of milestone '{}'. Problematic origins: {}",
                        milestone,
                        problematicTask.getName(),
                        milestone,
                        outerOrigins);
                candidates.remove(problematicTask);
            }
        }
    }

    /**
     * Basically the Kahn's algorithm from Wikipedia.
     */
    private <X> List<X> topologicalSort(Graph<X> graph) {
        // start with roots
        Queue<X> safeToVisit = graph.nodes()
                .stream()
                .filter(node -> graph.inDegree(node) == 0)
                .collect(Collectors.toCollection(ArrayDeque::new));
        // not roots with its dependants number as value
        Map<X, Integer> toVisit = graph.nodes().stream()
                .filter(node -> graph.inDegree(node) > 0)
                .collect(toMap(node -> node, graph::inDegree));

        List<X> sorted = new ArrayList<>(graph.nodes().size());
        while (!safeToVisit.isEmpty()) {
            X visitingNode = safeToVisit.remove();
            sorted.add(visitingNode);

            // visit all dependencies and decrease their dependant counter
            Set<X> dependencies = graph.successors(visitingNode);
            for (X dependency : dependencies) {
                toVisit.put(dependency, toVisit.get(dependency) - 1);

                // if a node was visited by all dependants, it's safe to visit
                if (toVisit.get(dependency) == 0) {
                    toVisit.remove(dependency);
                    safeToVisit.add(dependency);
                }
            }
        }

        return sorted;
    }
}
