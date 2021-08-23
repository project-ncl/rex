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
package org.jboss.pnc.rex.core.jobs;

import org.jboss.pnc.rex.model.Task;

import javax.enterprise.event.TransactionPhase;
import java.util.Optional;

/**
 * Template for creating Controller Jobs. Usually a ControllerJob is associated with a specific Task which triggered
 * said Job.
 *
 * Each Controller Job has to have a TransactionPhase defined. TransactionPhase will determine in which
 * transaction phase the Job is run (f.e. a Job should be executed only after a successful Transaction or a Job has to
 * be executed within the bounds of the Transaction which scheduled the Job). Jobs that run after Transactions have to
 * handle fault tolerance (Retries) and creating new transactions on their own.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public abstract class ControllerJob implements Runnable {

    protected TransactionPhase invocationPhase;

    protected Task context;

    protected ControllerJob(TransactionPhase invocationPhase, Task context) {
        this.invocationPhase = invocationPhase;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            beforeExecute();
            if (!execute()) return;
        } catch (RuntimeException e) {
            onException(e);
            throw e;
        } finally {
            afterExecute();
        }
    }

    abstract void beforeExecute();
    abstract void afterExecute();
    abstract boolean execute();
    abstract void onException(Throwable e);
    public TransactionPhase getInvocationPhase() {
        return invocationPhase;
    }
    public Optional<Task> getContext() {
        return context == null ? Optional.empty() : Optional.of(context);
    }
}
