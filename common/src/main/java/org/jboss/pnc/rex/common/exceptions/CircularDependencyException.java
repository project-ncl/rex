package org.jboss.pnc.rex.common.exceptions;

public class CircularDependencyException extends RuntimeException {
    public CircularDependencyException() {
        super();
    }

    public CircularDependencyException(String message) {
        super(message);
    }

    public CircularDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CircularDependencyException(Throwable cause) {
        super(cause);
    }
}
