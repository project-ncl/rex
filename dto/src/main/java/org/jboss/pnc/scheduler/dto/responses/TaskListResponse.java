package org.jboss.pnc.scheduler.dto.responses;

import lombok.Data;
import org.jboss.pnc.scheduler.dto.TaskDTO;

import java.util.List;

@Data
public class TaskListResponse {

    private List<TaskDTO> taskDTOS;
}
