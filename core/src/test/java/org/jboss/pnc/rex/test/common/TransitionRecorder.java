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
package org.jboss.pnc.rex.test.common;

import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.jobs.NotifyCallerJob;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.TransitionTime;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
@Slf4j
public class TransitionRecorder {

    private final Map<Set<String>, BlockingQueue<Tuple<String, State>>> subscriptions = new ConcurrentHashMap<>();

    private final Set<Tuple<String, State>> records = new ConcurrentHashSet<>();

    private final Map<String, Set<Tuple<TransitionTime, Task>>> snapshots = new ConcurrentHashMap<>();

    private final Map<String, Map<State, List<BlockingQueue<Task>>>> subscriptionDos = new ConcurrentHashMap<>();

    void recordTransition(@Observes(during = TransactionPhase.AFTER_SUCCESS) NotifyCallerJob transitionJob) {
        recordSnapshot(transitionJob);
        extractTransitionAndRecord(transitionJob);
        secondarySubscription(transitionJob);
    }

    private void recordSnapshot(NotifyCallerJob transitionJob) {
        TransitionTime tt = transitionJob.getContext()
                .orElseThrow()
                .getTimestamps()
                .stream()
                    .filter(ttime -> ttime.getTransition() == transitionJob.getTransition())
                    .max(Comparator.naturalOrder())
                    .get();
        Task task = transitionJob.getContext().get().toBuilder().build();

        Tuple<TransitionTime, Task> snapshot = new Tuple<>(tt, task);

        if (!snapshots.containsKey(task.getName())) {
            snapshots.put(task.getName(), new ConcurrentHashSet<>());
        }

        snapshots.get(task.getName()).add(snapshot);
    }

    private void secondarySubscription(NotifyCallerJob transitionJob) {
        TransitionTime tt = transitionJob.getContext()
                .orElseThrow()
                .getTimestamps()
                .stream()
                .filter(ttime -> ttime.getTransition() == transitionJob.getTransition())
                .max(Comparator.naturalOrder()).get();
        Task task = transitionJob.getContext().get().toBuilder().build();

        if (subscriptionDos.containsKey(task.getName()) &&
                subscriptionDos.get(task.getName()).containsKey(tt.getTransition().getAfter())) {
            subscriptionDos.get(task.getName()).get(tt.getTransition().getAfter()).forEach(queue -> queue.add(task));
        }
    }

    private void extractTransitionAndRecord(NotifyCallerJob transitionJob) {
        Tuple<String, State> state = new Tuple<>(
                transitionJob.getContext().get().getName(),
                transitionJob.getTransition().getAfter()
        );

        if (transitionJob.getTransition().getAfter().isFinal()) {
            log.info("Adding state {}", state);
            records.add(state);
            for (Set<String> subscription : subscriptions.keySet()) {
                if (subscription.contains(transitionJob.getContext().get().getName())) {
                    subscriptions.get(subscription).add(state);
                }
            }
        }
    }

    public void clear() {
        for (BlockingQueue<Tuple<String, State>> value : subscriptions.values()) {
            value.drainTo(new ArrayList<>());
        }
        subscriptions.clear();
        subscriptionDos.clear();
        records.clear();
        snapshots.clear();
    }

    public BlockingQueue<Tuple<String, State>> subscribe(Collection<String> subscription) {
        BlockingQueue<Tuple<String, State>> queue = new ArrayBlockingQueue<>(subscription.size());
        ConcurrentHashSet<String> concurrentSub = new ConcurrentHashSet<>(subscription.size());
        concurrentSub.addAll(subscription);
        subscriptions.put(concurrentSub, queue);

        // go through all records up until now to make sure subscriber didn't miss a transition
        for (var record : records) {
            if (subscription.contains(record.first())) {
                queue.add(record);
            }
        }
        return queue;
    }

    public BlockingQueue<Task> subscribeForTaskState(String taskName, State state) {
        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(10);
        if (!subscriptionDos.containsKey(taskName)) {
            subscriptionDos.put(taskName, new ConcurrentHashMap<>());
        }
        if (!subscriptionDos.get(taskName).containsKey(state)) {
            subscriptionDos.get(taskName).put(state, new CopyOnWriteArrayList<>());
        }
        subscriptionDos.get(taskName).get(state).add(queue);

        // go through all records up until now to make sure subscriber didn't miss a transition
        if (snapshots.containsKey(taskName)) {
            for (var tuple : snapshots.get(taskName)) {
                if (tuple.first.getTransition().getAfter() == state) {
                    queue.add(tuple.second);
                }
            }
        }
        return queue;
    }

    public Map<String, SortedSet<Tuple<TransitionTime, Task>>> snapshots() {
        Map<String, SortedSet<Tuple<TransitionTime, Task>>> toReturn = new ConcurrentHashMap<>(snapshots.size());
        snapshots.forEach((key, value) -> {
            var sset = new TreeSet<Tuple<TransitionTime, Task>>(Comparator.comparing(Tuple::first));
            sset.addAll(value);
            toReturn.put(key, sset);
        });
        return toReturn;
    }

    public int count(String taskName, State state) {
        if (snapshots.get(taskName) == null) {
            return 0;
        };

        int count = 0;
        var transitions = snapshots.get(taskName);
        for (var transition : transitions) {
            State after = transition.first.getTransition().getAfter();
            if (after == state) count++;
        }

        return count;
    }

    public record Tuple<T1, T2>(T1 first, T2 second) {}
}
