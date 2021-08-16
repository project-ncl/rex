package org.jboss.pnc.rex.common.exceptions;

public class TaskMissingException extends RuntimeException {
    public TaskMissingException() {
    }

    public TaskMissingException(final String msg) {
        super(msg);
    }

    public TaskMissingException(final Throwable cause) {
        super(cause);
    }

    public TaskMissingException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
