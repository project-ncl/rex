package org.jboss.pnc.scheduler.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FinishRequest {

    public Boolean status;

    public Object response;

    public Boolean getStatus() {
        return status;
    }
}
