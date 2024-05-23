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
package org.jboss.pnc.rex.core.api;

/**
 * Interface for interacting with internal queue. Each queue has 2 counters. Maximum counter which limits maximum amount
 * of concurrently running Tasks, and Running counter which signifies current number of concurrently running Tasks.
 *
 * If the amount of running Tasks is higher than the maximum amount, Tasks that are able to start are left in the queue
 * (in the ENQUEUED state).
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
public interface QueueManager {
    /**
     * The method checks whether there is a room to schedule new Tasks. If the answer is no, the method does nothing,
     * otherwise, maximum possible amount of Tasks in {@link ENQUEUED} state is transitioned to {@link STARTING}.
     */
    void poke();

    /**
     * Decrease amount of running counter by one. The method is invoked when a Task transitions from {@link RUNNING}
     * state into {@link FINAL}.
     */
    void decreaseRunningCounter();

    /**
     * The method changes the maximum amount of concurrently running Tasks. If the amount is lower than the number of
     * currently running Tasks, the mentioned Tasks are unaffected but no new Tasks are scheduled. The queue is poked
     * after (and potentially starting {@link ENQUEUED} Tasks).
     *
     * @param amount new amount of maximum concurrent running Tasks
     */
    void setMaximumConcurrency(Long amount);

    /**
     * Returns current number in the maximum counter.
     *
     * @return maximum counter value
     */
    Long getMaximumConcurrency();

    /**
     * Returns current number in the running counter. This value should reflect amount of running Tasks.
     *
     * @return
     */
    Long getRunningCounter();

    /**
     * The method queries running tasks and synchronizes running counter in case it is different.
     *
     * @return returns synchronized value of the counter
     */
    Long synchronizeRunningCounter();
}
