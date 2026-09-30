/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.exceptions;

public class ConstraintConflictException extends RuntimeException {

    private final String constraint;

    public ConstraintConflictException(String constraint) {
        super();
        this.constraint = constraint;
    }

    public ConstraintConflictException(String message, String constraint) {
        super(message);
        this.constraint = constraint;
    }

    public ConstraintConflictException(String message, Throwable cause, String constraint) {
        super(message, cause);
        this.constraint = constraint;
    }

    public ConstraintConflictException(Throwable cause, String constraint) {
        super(cause);
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }

}
