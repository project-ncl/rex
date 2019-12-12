package org.jboss.pnc.scheduler.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class RemoteLinksDTO {

    private String startUrl;

    private String stopUrl;
}
