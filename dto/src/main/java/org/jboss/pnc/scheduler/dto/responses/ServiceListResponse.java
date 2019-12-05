package org.jboss.pnc.scheduler.dto.responses;

import lombok.*;
import org.jboss.pnc.scheduler.dto.ServiceDTO;

import java.util.List;

@Data
public class ServiceListResponse {

    private List<ServiceDTO> serviceDTOS;
}
