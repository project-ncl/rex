/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

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
}
