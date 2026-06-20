package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
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
import com.mahmutsalih.task_management.security.CurrentUserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void create_shouldCreateTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User user = User.builder().id(2L).firstName("Mahmut").lastName("Kelkit").build();
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .description("Write service tests")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 1, 15))
                .projectId(1L)
                .assignedUserId(2L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            task.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return task;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getTitle()).isEqualTo("Create tests");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getAssignedUserId()).isEqualTo(2L);
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void getById_shouldReturnTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User user = User.builder().id(2L).firstName("Mahmut").lastName("Kelkit").build();
        Task task = Task.builder()
                .id(3L)
                .title("Create tests")
                .description("Write service tests")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .assignedUser(user)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getById(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getProjectName()).isEqualTo("Project");
        assertThat(response.getAssignedUserFullName()).isEqualTo("Mahmut Kelkit");
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void getById_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
    }
}
