package org.jboss.pnc.scheduler.common.exceptions;

public class InvalidServiceDeclarationException extends RuntimeException {
    public InvalidServiceDeclarationException() {
        super();
    }

    public InvalidServiceDeclarationException(String message) {
        super(message);
    }

    public InvalidServiceDeclarationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidServiceDeclarationException(Throwable cause) {
        super(cause);
    }

    protected InvalidServiceDeclarationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
