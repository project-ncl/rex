/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

public class RequestRetryException extends RuntimeException{

    public RequestRetryException(String message) {
        super(message);
    }

    public RequestRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestRetryException(Throwable cause) {
        super(cause);
    }
}
