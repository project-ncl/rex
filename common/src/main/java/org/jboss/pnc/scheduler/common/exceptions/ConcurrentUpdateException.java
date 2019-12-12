package org.jboss.pnc.scheduler.common.exceptions;

public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException() {
        super();
    }

    public ConcurrentUpdateException(String message) {
        super(message);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentUpdateException(Throwable cause) {
        super(cause);
    }

    protected ConcurrentUpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
