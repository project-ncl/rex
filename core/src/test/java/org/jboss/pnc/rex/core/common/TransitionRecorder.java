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
package org.jboss.pnc.rex.core.common;

import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.jobs.ChainingJob;
import org.jboss.pnc.rex.core.jobs.NotifyCallerJob;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class TransitionRecorder {

    private final Map<Set<String>, BlockingQueue<Tuple<String, State>>> subscriptions = new ConcurrentHashMap<>();

    private final Set<Tuple<String, State>> records = new ConcurrentHashSet<>();

    void recordTransition(@Observes(during = TransactionPhase.AFTER_SUCCESS) NotifyCallerJob transitionJob) {
        extractTransitionAndRecord(transitionJob);
    }

    void recordTransition(@Observes(during = TransactionPhase.AFTER_SUCCESS) ChainingJob chainingJob) {
        if (chainingJob.getChainLinks().get(0) instanceof NotifyCallerJob notification) {
            extractTransitionAndRecord(notification);
        }
    }

    private void extractTransitionAndRecord(NotifyCallerJob transitionJob) {
        Tuple<String, State> state = new Tuple<>(
                transitionJob.getContext().get().getName(),
                transitionJob.getTransition().getAfter());

        if (transitionJob.getTransition().getAfter().isFinal()) {
            log.info("Adding state {}", state.toString());
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
        records.clear();
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

    public record Tuple<T1,T2>(
    T1 first, T2 second)
    {
    }
}
