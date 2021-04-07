package org.jboss.pnc.scheduler.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.jboss.pnc.scheduler.dto.EdgeDTO;

import java.util.Map;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGraphRequest {

    @Singular
    public Set<EdgeDTO> edges;

    @Singular
    public Map<String, CreateTaskDTO> vertices;
}
