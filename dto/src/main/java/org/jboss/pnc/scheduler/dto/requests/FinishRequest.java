package org.jboss.pnc.scheduler.dto.requests;

import lombok.Data;

@Data
public class FinishRequest {

    Boolean status;

    String result;
}
