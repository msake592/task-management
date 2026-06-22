package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
import com.mahmutsalih.task_management.dto.request.TaskUpdateRequest;
import com.mahmutsalih.task_management.dto.response.TaskResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Role;
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
import org.springframework.security.access.AccessDeniedException;
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
    void create_whenAdminAssignsAnotherUser_shouldCreateTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User admin = adminUser();
        User user = regularUser(2L, "mahmut@example.com");
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
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
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
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void create_whenAdminDoesNotSendAssignedUserId_shouldCreateUnassignedTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User admin = adminUser();
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .description("Write service tests")
                .projectId(1L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            task.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return task;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getAssignedUserId()).isNull();
        assertThat(response.getAssignedUsername()).isNull();
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void create_whenUserDoesNotSendAssignedUserId_shouldAssignToCurrentUser() {
        Project project = Project.builder().id(1L).name("Project").build();
        User currentUser = regularUser(2L, "mahmut@example.com");
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .description("Write service tests")
                .projectId(1L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            task.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return task;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.getAssignedUserId()).isEqualTo(2L);
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void create_whenUserAssignsToSelf_shouldCreateTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User currentUser = regularUser(2L, "mahmut@example.com");
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .description("Write service tests")
                .projectId(1L)
                .assignedUserId(2L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            task.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return task;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.getAssignedUserId()).isEqualTo(2L);
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
    }

    @Test
    void create_whenUserAssignsAnotherUser_shouldThrowAccessDeniedException() {
        Project project = Project.builder().id(1L).name("Project").build();
        User currentUser = regularUser(2L, "mahmut@example.com");
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .projectId(1L)
                .assignedUserId(99L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Users can only assign tasks to themselves.");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void create_whenAssignedUserNotFound_shouldThrowResourceNotFoundException() {
        Project project = Project.builder().id(1L).name("Project").build();
        User admin = adminUser();
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .projectId(1L)
                .assignedUserId(99L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void create_whenProjectIsNotAccessible_shouldThrowAccessDeniedException() {
        Project project = Project.builder().id(1L).name("Other Project").build();
        TaskRequest request = TaskRequest.builder()
                .title("Create tests")
                .projectId(1L)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(currentUserService).validateProjectAccess(project);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this resource");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getById_shouldReturnTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User user = regularUser(2L, "mahmut@example.com");
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
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
        assertThat(response.getAssignedUserFullName()).isEqualTo("Mahmut Kelkit");
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void update_shouldReturnUpdatedAt() {
        Project oldProject = Project.builder().id(1L).name("Old Project").build();
        Project project = Project.builder().id(2L).name("Project").build();
        User admin = adminUser();
        Task task = Task.builder()
                .id(3L)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(oldProject)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 2, 1))
                .projectId(2L)
                .assignedUserId(null)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(3L, request);

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(currentUserService).validateProjectAccess(oldProject);
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void update_whenUserChangesAssignedUser_shouldThrowAccessDeniedException() {
        Project project = Project.builder().id(1L).name("Project").build();
        User currentUser = regularUser(2L, "mahmut@example.com");
        User assignedUser = regularUser(2L, "mahmut@example.com");
        Task task = Task.builder()
                .id(3L)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .assignedUser(assignedUser)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .projectId(1L)
                .assignedUserId(99L)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> taskService.update(3L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Users cannot change task assignee.");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void update_whenUserOmitsAssignedUserId_shouldKeepCurrentAssignee() {
        Project project = Project.builder().id(1L).name("Project").build();
        User currentUser = regularUser(2L, "mahmut@example.com");
        Task task = Task.builder()
                .id(3L)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .assignedUser(currentUser)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .projectId(1L)
                .assignedUserId(null)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(3L, request);

        assertThat(response.getAssignedUserId()).isEqualTo(2L);
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
    }

    @Test
    void update_whenAdminChangesAssignedUser_shouldUpdateAssignee() {
        Project project = Project.builder().id(1L).name("Project").build();
        User admin = adminUser();
        User assignee = regularUser(4L, "assignee@example.com");
        Task task = Task.builder()
                .id(3L)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .projectId(1L)
                .assignedUserId(4L)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(4L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(3L, request);

        assertThat(response.getAssignedUserId()).isEqualTo(4L);
        assertThat(response.getAssignedUsername()).isEqualTo("assignee@example.com");
    }

    @Test
    void getById_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
    }

    private User adminUser() {
        return User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Role.builder().name("ADMIN").build())
                .build();
    }

    private User regularUser(Long id, String email) {
        return User.builder()
                .id(id)
                .firstName("Mahmut")
                .lastName("Kelkit")
                .email(email)
                .role(Role.builder().name("USER").build())
                .build();
    }
}
