package org.jboss.pnc.scheduler.dto.responses;

import lombok.*;

@Data
public class ErrorResponse {

    private String errorType;

    private String errorMessage;

    public ErrorResponse(Exception e) {
        this.errorType = e.getClass().getSimpleName();
        this.errorMessage = e.getMessage();
    }
}
