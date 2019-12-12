package org.jboss.pnc.scheduler.common.exceptions;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException() {
    }

    public ServiceNotFoundException(final String msg) {
        super(msg);
    }

    public ServiceNotFoundException(final Throwable cause) {
        super(cause);
    }

    public ServiceNotFoundException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
