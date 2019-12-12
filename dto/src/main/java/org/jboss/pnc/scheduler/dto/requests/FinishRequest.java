package org.jboss.pnc.scheduler.dto.requests;

import lombok.Data;

public class FinishRequest {

    private Boolean status;

    public FinishRequest(Boolean status) {
        this.status = status;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
