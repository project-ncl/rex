/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
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
    FINAL,
    /**
     * Task triggered rollback process from Milestone.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    ROLLBACK_TODO,
    /**
     * Task is actively participating in rollback process.
     * <p>
     * In this state you are unable to add additional dependencies.
     */
    ROLLBACK
}