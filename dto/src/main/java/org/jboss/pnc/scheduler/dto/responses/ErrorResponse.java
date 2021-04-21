package org.jboss.pnc.scheduler.dto.responses;

import lombok.Data;

@Data
public class ErrorResponse {

    public String errorType;

    public String errorMessage;

    public ErrorResponse(Exception e) {
        this.errorType = e.getClass().getSimpleName();
        this.errorMessage = e.getMessage();
    }
}
