package org.jboss.pnc.scheduler.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class RemoteLinksDTO {

    public String startUrl;

    public String stopUrl;

    public RemoteLinksDTO() {
    }
}
