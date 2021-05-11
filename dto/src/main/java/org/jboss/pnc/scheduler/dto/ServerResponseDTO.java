package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.common.enums.State;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponseDTO {

    public State state;

    public Boolean positive;

    public Object body;
}
