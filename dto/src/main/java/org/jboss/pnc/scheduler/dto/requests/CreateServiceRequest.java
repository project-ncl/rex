package org.jboss.pnc.scheduler.dto.requests;

import lombok.Data;
import org.jboss.pnc.scheduler.dto.ServiceDTO;

import java.util.List;

@Data
public class CreateServiceRequest {

    private final List<ServiceDTO> services;
}
