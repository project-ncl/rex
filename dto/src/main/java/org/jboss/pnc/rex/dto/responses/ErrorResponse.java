/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.dto.responses;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;

@Data
@NoArgsConstructor
public class ErrorResponse {

    public String errorType;

    public String errorMessage;

    public Object object;

    public String stackTrace;

    public ErrorResponse(Exception e) {
        this.errorType = e.getClass().getSimpleName();
        this.errorMessage = e.getMessage();
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        e.printStackTrace(pw);
        pw.flush();
        this.stackTrace = w.toString();
    }
    public ErrorResponse(Exception e, Object object) {
        this(e);
        this.object = object;
    }

    public ErrorResponse(String errorType, String errorMessage, Object object) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.object = object;
    }
}
