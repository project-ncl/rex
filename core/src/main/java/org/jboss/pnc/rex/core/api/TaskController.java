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
package org.jboss.pnc.rex.core.api;

import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.Origin;

import java.time.Instant;

/**
 * This is API for TaskController.
 * <p>
 * TaskController is the entity that handles transitions and schedules internal Jobs for each Task. It is a public API
 * for interacting with Tasks.
 *
 * <p>
 * TaskController does not hold any data.
 *
 * @author Jan Michalov {@literal <jmichalo@redhat.com>}
 */
public interface TaskController {
    /**
     * Sets mode of a Task. Needs to be called in a transaction.
     *
     * @param name id of the Task
     * @param mode the mode
     */
    void setMode(String name, Mode mode);

    /**
     * Sets mode of a Task. Needs to be called in a transaction. Additionally, pokes queue
     * after transaction succeeds if specified.
     *
     * @param name id of the Task
     * @param mode the mode
     * @param pokeQueue should pokeQueue
     */
    void setMode(String name, Mode mode, boolean pokeQueue);

    /**
     * Method used for positive callback. Needs to be called in a transaction.
     * <p>
     * f.e. to signalize that remote Task has started/cancelled/finished.
     *
     * @param name     id of the Task
     * @param origin   the origin of response
     * @param isRollback callback is from rollback endpoint
     */
    void accept(String name, Object response, Origin origin, boolean isRollback);

    /**
     * Method used for negative callback. Needs to be called in a transaction.
     * <p>
     * f.e. to signalize that remote Task failed to start/cancel or failed during execution.
     *
     * @param name     id of the Task
     * @param origin   the origin of response
     * @param isRollback callback is from rollback endpoint
     */
    void fail(String name, Object response, Origin origin, boolean isRollback);

    /**
     * Registers a beat of remotely running Task.
     *
     * @param name id of the Task
     * @param response response of the beat
     * @param time earliest time that beat arrived
     */
    void beat(String name, Object response, Instant time);

    void dequeue(String name);

    /**
     * Deletes a Task. The method deletes also cascades on dependencies.
     *
     * A Task can be removed only if all of its dependants are removed and is marked for disposal.
     *
     * @param name id of the Task
     */
    void delete(String name);

    /**
     * Marks the Task for disposal/cleaning.
     *
     * @param name        id of the Task
     * @param pokeCleaner
     */
    void markForDisposal(String name, boolean pokeCleaner);

    /**
     * Clears a secondary constraint of a Task (if one is present). The method does nothing if the constraint was
     * already cleared.
     *
     * @param name id of the Task
     */
    void clearConstraint(String name);

    void reset(String name);

    /**
     * Prepare this Task's metadata without any transitions. It will set expected number of dependants that this Task
     * should wait for and expected number of running dependencies after rollback process finishes. It's important that
     * these numbers are consistent because it may lead to stuck workflow.
     *
     * @param name id of the Task
     * @param rollbackDependants number of expected active rollbacking dependants
     * @param dependencies number of future running dependencies
     */
    void primeForRollback(String name, int rollbackDependants, int dependencies);

    /**
     * Increase trigger counter and start rollback process from the Milestone task.
     *
     * @param name id of the Task
     */
    void rollbackTriggered(String name);

    /**
     * Called on the milestone Task. It will recursively transition all dependants into correct StateGroup.ROLLBACK
     * states (TO_ROLLBACK, ROLLBACK_REQUESTED, ROLLEDBACK...).
     *
     * @param name id of the Task
     */
    void startRollbackProcess(String name);

    /**
     * This method changes nothing on the Task but it will force the Task to participate in the Thread's transaction. If
     * there is a concurrent change in another transaction, this or the other transaction will fail and restart with
     * refreshed data.
     *
     * This method should be used if making critical decisions based on the state of this Task without modifying it.
     *
     * @param name id of the Task
     */
    void involveInTransaction(String name);
}