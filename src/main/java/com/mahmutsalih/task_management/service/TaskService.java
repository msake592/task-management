package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
import com.mahmutsalih.task_management.dto.request.TaskUpdateRequest;
import com.mahmutsalih.task_management.dto.response.TaskResponse;
import com.mahmutsalih.task_management.enums.TaskStatus;
import java.util.List;

public interface TaskService {

    TaskResponse create(TaskRequest request);

    TaskResponse getById(Long id);

    List<TaskResponse> getAll();

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);

    TaskResponse updateStatus(Long id, TaskStatus status);

    TaskResponse assignToUser(Long id, Long userId);
}
