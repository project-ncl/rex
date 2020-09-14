package org.jboss.pnc.scheduler.common.exceptions;

public class InvalidTaskDeclarationException extends RuntimeException {
    public InvalidTaskDeclarationException() {
        super();
    }

    public InvalidTaskDeclarationException(String message) {
        super(message);
    }

    public InvalidTaskDeclarationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTaskDeclarationException(Throwable cause) {
        super(cause);
    }

    protected InvalidTaskDeclarationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
