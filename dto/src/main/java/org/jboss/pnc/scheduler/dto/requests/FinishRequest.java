package org.jboss.pnc.scheduler.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FinishRequest {

    @NotNull
    public Boolean status;

    public Object response;

    public Boolean getStatus() {
        return status;
    }
}
