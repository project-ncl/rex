package org.jboss.pnc.scheduler.dto.requests;

import org.jboss.pnc.scheduler.dto.TaskDTO;

import java.util.List;

public class CreateTaskRequest {

    public List<TaskDTO> tasks;

    public CreateTaskRequest(List<TaskDTO> tasks) {
        this.tasks = tasks;
    }

    public CreateTaskRequest() {
    }

    public List<TaskDTO> getTasks() {
        return tasks;
    }
}
