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
package org.jboss.pnc.rex.core.jobs;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.rex.model.Task;

import java.util.Iterator;
import java.util.List;

@Slf4j
public class TreeJob extends ControllerJob {

    @Getter
    private final ControllerJob root;
    private final ValueGraph<ControllerJob, ChainTrigger> graph;
    private final Event<ControllerJob> jobEvent;

    /**
     * Special job that consists of Controller Jobs connected by edges like in
     *
     * @param root
     * @param invocationPhase
     * @param context
     * @param async
     * @param graph
     */
    private TreeJob(ControllerJob root,
                   TransactionPhase invocationPhase,
                   Task context,
                   boolean async,
                   ValueGraph<ControllerJob, ChainTrigger> graph) {
        super(invocationPhase, context, async);
        this.graph = graph;
        this.root = root;
        var event = new TypeLiteral<Event<ControllerJob>>() {};
        this.jobEvent = CDI.current().select(event).get();
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        Iterable<ControllerJob> bfs = Traverser.forTree(new ChainSuccessorFunction(graph)).breadthFirst(root);

        Iterator<ControllerJob> iterator = bfs.iterator();
        ControllerJob job = null;

        // iterate through to avoid lazy loading of ChainSuccessorFunction::successors function
        while (iterator.hasNext()) {
            job = iterator.next();
        }

        return job != null && job.isSuccessful();
    }

    @Override
    protected void onFailure() {}

    @Override
    protected void onException(Throwable e) {}

    private enum ChainTrigger {
        ON_SUCCESS,
        ON_FAILURE,
        ON_ANY_OUTCOME
    }

    public static TreeJobBuilder of(ControllerJob root) {
        return new TreeJobBuilder(root);
    }
    public static TreeJobBuilder of(ControllerJob root, TransactionPhase invocationPhase, Task context, boolean async) {
        return new TreeJobBuilder(root, invocationPhase, context, async);
    }

    private class ChainSuccessorFunction implements SuccessorsFunction<ControllerJob> {
        private final ValueGraph<ControllerJob, ChainTrigger> graph;

        private ChainSuccessorFunction(ValueGraph<ControllerJob, ChainTrigger> graph) {
            this.graph = graph;
        }

        @Override
        public Iterable<? extends ControllerJob> successors(ControllerJob parent) {
            //run the job DURING traversal
            if (!parent.isFinished()) {
                try {
                    log.info("Running {}", parent);
                    jobEvent.fire(parent);
                } catch (Exception e) {
                    log.warn("Job {} has thrown an exception. Executing Job defined in the TreeJob regardless.", parent, e);
                }
            }

            if (!parent.isFinished()) {
                throw new IllegalStateException("Job has been run but is not marked as finished. " + parent);
            }

            boolean parentResult = parent.isSuccessful();
            List<ControllerJob> list = graph.successors(parent).stream().filter(
                (job) -> {
                    ChainTrigger trigger = graph.edgeValue(parent, job)
                        .orElseThrow(() -> new IllegalStateException("Missing trigger between " + parent + " and " + job));

                    return switch (trigger) {
                        case ON_SUCCESS -> parentResult;
                        case ON_FAILURE -> !parentResult;
                        case ON_ANY_OUTCOME -> true;
                    };
                }
            ).toList();
            return list;
        }
    }

    public static class TreeJobBuilder {
        private final ControllerJob root;
        private final MutableValueGraph<ControllerJob, ChainTrigger> graph;
        private final Task context;
        private final boolean async;
        private final TransactionPhase invocationPhase;

        public TreeJobBuilder(ControllerJob root) {
            this(root, root.invocationPhase, root.context, root.async);
        }

        public TreeJobBuilder(ControllerJob root, TransactionPhase invocationPhase, Task context, boolean async) {
            if (root == null) {
                throw new IllegalArgumentException("Root must not be null");
            }
            root.invocationPhase = TransactionPhase.IN_PROGRESS;
            root.async = false;
            this.root = root;
            this.invocationPhase = invocationPhase;
            this.context = context;
            this.async = async;
            this.graph = ValueGraphBuilder.directed()
                    .incidentEdgeOrder(ElementOrder.stable())
                    .nodeOrder(ElementOrder.insertion())
                    .build();

            graph.addNode(root);
        }

        public TreeJobBuilder triggerAfter(ControllerJob parent, ControllerJob child) {
            return triggerAfter(parent, child, TreeJob.ChainTrigger.ON_ANY_OUTCOME);
        }

        public TreeJobBuilder triggerAfterSuccess(ControllerJob parent, ControllerJob child) {
            return triggerAfter(parent, child, TreeJob.ChainTrigger.ON_SUCCESS);
        }

        public TreeJobBuilder triggerAfterFailure(ControllerJob parent, ControllerJob child) {
            return triggerAfter(parent, child, TreeJob.ChainTrigger.ON_FAILURE);
        }

        private TreeJobBuilder triggerAfter(ControllerJob parent, ControllerJob child, ChainTrigger trigger) {
            if (parent == null) throw new IllegalArgumentException("Parent node must not be null.");
            if (child == null) throw new IllegalArgumentException("Child node must not be null.");
            if (trigger == null) throw new IllegalArgumentException("Trigger must not be null.");
            if (parent == child) throw new IllegalArgumentException("Parent cannot be the same instance as child");
            if (!graph.nodes().contains(parent)) {
                throw new IllegalArgumentException("Parent is not present in the tree graph");
            }
            if (graph.nodes().contains(child)) {
                throw new IllegalArgumentException("Child can't be present in the tree graph");
            }
            child.invocationPhase = TransactionPhase.IN_PROGRESS;
            child.async = false;
            graph.putEdgeValue(parent, child, trigger);

            return this;
        }

        public TreeJob build() {
            return new TreeJob(root, invocationPhase, context, async, ImmutableValueGraph.copyOf(graph));
        }
    }

    @Override
    public String toString() {
        return graph.toString();
    }
}
