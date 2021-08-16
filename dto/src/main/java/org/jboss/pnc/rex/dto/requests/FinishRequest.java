package org.jboss.pnc.rex.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FinishRequest {

    @NotNull
    public Boolean status;

    public Object response;

    public Boolean getStatus() {
        return status;
    }
}
