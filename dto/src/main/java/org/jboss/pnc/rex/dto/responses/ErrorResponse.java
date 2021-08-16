package org.jboss.pnc.rex.dto.responses;

import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;

@Data
public class ErrorResponse {

    public String errorType;

    public String errorMessage;

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
}
