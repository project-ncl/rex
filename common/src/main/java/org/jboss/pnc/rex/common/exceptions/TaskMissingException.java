/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

public class TaskMissingException extends RuntimeException {

    private final String taskName;

    public TaskMissingException(String taskName) {
        this.taskName = taskName;
    }

    public TaskMissingException(final String msg, String taskName) {
        super(msg);
        this.taskName = taskName;
    }

    public TaskMissingException(final Throwable cause, String taskName) {
        super(cause);
        this.taskName = taskName;
    }

    public TaskMissingException(final String msg, final Throwable cause, String taskName) {
        super(msg, cause);
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }
}
