package org.jboss.pnc.scheduler.common.exceptions;

public class TaskConflictException extends RuntimeException {
    public TaskConflictException() {
        super();
    }

    public TaskConflictException(String message) {
        super(message);
    }

    public TaskConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskConflictException(Throwable cause) {
        super(cause);
    }
}
