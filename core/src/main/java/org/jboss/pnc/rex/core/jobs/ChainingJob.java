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

import lombok.Getter;
import org.jboss.pnc.rex.model.Task;

import jakarta.enterprise.event.TransactionPhase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chained Jobs are executed SEQUENTIALLY in the order they were added to the chain. Jobs can trigger based on the
 * result of previous Job from the sequence. For example, if the job succeeded or failed.
 *
 * The chaining and triggers rely on the return values of execute() method. Therefore, the Jobs must return proper
 * boolean to see whether the execution failed or succeeded.
 */
public class ChainingJob extends ControllerJob {

    @Getter
    private final List<ControllerJob> chainLinks = new ArrayList<>();

    private final Map<ControllerJob, ChainTrigger> chainTriggers = new HashMap<>();

    /**
     * Copy the execution profile from the initial task in chain.
     *
     * Throws NPE if firstChain is null.
     *
     * @param firstChain first task in chain
     */
    public ChainingJob(ControllerJob firstChain) {
        super(firstChain.invocationPhase, firstChain.context, firstChain.async);

        chain(firstChain, ChainTrigger.ON_ANY_OUTCOME);
    }

    public ChainingJob(TransactionPhase invocationPhase, Task context, boolean async, ControllerJob firstChain) {
        super(invocationPhase, context, async);

        chain(firstChain, ChainTrigger.ON_ANY_OUTCOME);
    }

    public static ChainingJob of(ControllerJob firstChain) {
        return new ChainingJob(firstChain);
    }

    public ChainingJob chain(ControllerJob link) {
        return chain(link, ChainTrigger.ON_ANY_OUTCOME);
    }

    public ChainingJob chainOnSuccess(ControllerJob link) {
        return chain(link, ChainTrigger.ON_SUCCESS);
    }

    public ChainingJob chainOnFailure(ControllerJob link) {
        return chain(link, ChainTrigger.ON_FAILURE);
    }

    private ChainingJob chain(ControllerJob link, ChainTrigger trigger) {
        if (link == null) {
            throw new IllegalArgumentException("Job link cannot be null");
        }
        chainLinks.add(link);
        chainTriggers.put(link, trigger);

        return this;
    }

    @Override
    protected void beforeExecute() {}

    @Override
    protected void afterExecute() {}

    @Override
    public boolean execute() {
        boolean lastResult = true;
        for (ControllerJob toRun : chainLinks) {
            ChainTrigger toRunTrigger = chainTriggers.get(toRun);

            boolean shouldRun = switch (toRunTrigger) {
                case ON_SUCCESS -> lastResult;
                case ON_FAILURE -> !lastResult;
                case ON_ANY_OUTCOME -> true;
            };

            if (!shouldRun) {
                break;
            }

            // RUN the task in chain
            toRun.run();

            lastResult = toRun.isSuccessful();
        }
        return lastResult;
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
}
