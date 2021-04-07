package org.jboss.pnc.scheduler.dto.responses;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.dto.TaskDTO;

import java.util.List;

@Data
@NoArgsConstructor
public class TaskListResponse {

    public List<TaskDTO> taskDTOS;
}
