/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.api.parameters;

public enum ErrorOption {
    /**
     * DEFAULT option
     *
     * In case there are unrecoverable errors, return ErrorResponse and appropriate status code.
     */
    PASS_ERROR,

    /**
     * In some cases (TaskMissing) return positive status code even if it's not default behaviour.
     */
    IGNORE
}
