package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
import com.mahmutsalih.task_management.dto.request.TaskUpdateRequest;
import com.mahmutsalih.task_management.dto.response.TaskResponse;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.enums.TaskPriority;
import com.mahmutsalih.task_management.enums.TaskStatus;
import org.springframework.data.domain.Page;

public interface TaskService {

    TaskResponse create(TaskRequest request);

    TaskResponse getById(Long id);

    Task getEntityById(Long id);

    Page<TaskResponse> getAll(
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            TaskPriority priority,
            Long projectId,
            Long assignedUserId
    );

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);

    TaskResponse updateStatus(Long id, TaskStatus status);

    TaskResponse assignToUser(Long id, Long userId);
}
