package org.jboss.pnc.scheduler.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.dto.TaskDTO;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSetResponse {

    public Set<TaskDTO> taskDTOS;
}
