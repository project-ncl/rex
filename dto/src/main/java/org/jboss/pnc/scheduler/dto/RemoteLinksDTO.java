package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RemoteLinksDTO {

    public String startUrl;

    public String stopUrl;

    public RemoteLinksDTO() {
    }
}
