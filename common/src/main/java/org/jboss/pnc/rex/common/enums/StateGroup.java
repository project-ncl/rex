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
package org.jboss.pnc.rex.common.enums;

/**
 * The enum State Group.
 */
public enum StateGroup {
    /**
     * Task is idle and has not started remote execution.
     * <p>
     * In this state you are able to add additional dependencies.
     */
    IDLE,
    /**
     * Task is waiting in queue and can be started at any time. It's a state between being idle and running.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    QUEUED,
    /**
     * Task is remotely active.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    RUNNING,
    /**
     * Task has finished execution or failed.
     * <p>
     * Transitions from RUNNING group into FINAL group will poke queue to start new Tasks.
     * <p>
     * In this state you are unable to add additional dependencies and cannot transition to any other state.
     */
    FINAL
}
