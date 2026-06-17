package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
import com.mahmutsalih.task_management.dto.request.TaskUpdateRequest;
import com.mahmutsalih.task_management.dto.response.TaskResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.enums.TaskPriority;
import com.mahmutsalih.task_management.enums.TaskStatus;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectRepository;
import com.mahmutsalih.task_management.repository.TaskRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import com.mahmutsalih.task_management.service.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public TaskResponse create(TaskRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .project(findProject(request.getProjectId()))
                .assignedUser(findUserOrNull(request.getAssignedUserId()))
                .build();

        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse getById(Long id) {
        return toResponse(findTask(id));
    }

    @Override
    public List<TaskResponse> getAll() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = findTask(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setProject(findProject(request.getProjectId()));
        task.setAssignedUser(findUserOrNull(request.getAssignedUserId()));

        return toResponse(taskRepository.save(task));
    }

    @Override
    public void delete(Long id) {
        Task task = findTask(id);
        taskRepository.delete(task);
    }

    @Override
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = findTask(id);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse assignToUser(Long id, Long userId) {
        Task task = findTask(id);
        task.setAssignedUser(findUser(userId));
        return toResponse(taskRepository.save(task));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private User findUserOrNull(Long id) {
        if (id == null) {
            return null;
        }

        return findUser(id);
    }

    private TaskResponse toResponse(Task task) {
        Project project = task.getProject();
        User assignedUser = task.getAssignedUser();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .projectId(project.getId())
                .projectName(project.getName())
                .assignedUserId(assignedUser != null ? assignedUser.getId() : null)
                .assignedUserFullName(assignedUser != null ? getFullName(assignedUser) : null)
                .build();
    }

    private String getFullName(User user) {
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            return user.getFirstName();
        }

        return user.getFirstName() + " " + user.getLastName();
    }
}
