package org.jboss.pnc.scheduler.common.exceptions;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
    }

    public TaskNotFoundException(final String msg) {
        super(msg);
    }

    public TaskNotFoundException(final Throwable cause) {
        super(cause);
    }

    public TaskNotFoundException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
