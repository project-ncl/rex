/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

public class QueueMissingException extends RuntimeException {

    private final String queueName;

    public QueueMissingException(String queueName) {
        this.queueName = queueName;
    }

    public QueueMissingException(final String msg, String queueName) {
        super(msg);
        this.queueName = queueName;
    }

    public QueueMissingException(final Throwable cause, String queueName) {
        super(cause);
        this.queueName = queueName;
    }

    public QueueMissingException(final String msg, final Throwable cause, String queueName) {
        super(msg, cause);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }
}
