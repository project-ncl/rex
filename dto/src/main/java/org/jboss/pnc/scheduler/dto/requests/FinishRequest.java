package org.jboss.pnc.scheduler.dto.requests;

public class FinishRequest {

    public Boolean status;

    public FinishRequest(Boolean status) {
        this.status = status;
    }

    public FinishRequest() {
    }

    public Boolean getStatus() {
        return status;
    }
}
