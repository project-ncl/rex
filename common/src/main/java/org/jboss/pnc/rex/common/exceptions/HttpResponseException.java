/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

/**
 * Representing http error responses.
 */
public class HttpResponseException extends RuntimeException {

    private int statusCode;

    public HttpResponseException(int statusCode) {
        super();
        this.statusCode = statusCode;
    }

    public HttpResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpResponseException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpResponseException(int statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
