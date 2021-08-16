package org.jboss.pnc.rex.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.rex.dto.TaskDTO;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSetResponse {

    public Set<TaskDTO> taskDTOS;
}
