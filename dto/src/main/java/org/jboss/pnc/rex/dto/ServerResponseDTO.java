package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jboss.pnc.rex.common.enums.State;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponseDTO {

    public State state;

    public Boolean positive;

    public Object body;
}
