package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.service.ProjectService;
import com.mahmutsalih.task_management.repository.TaskRepository;
import com.mahmutsalih.task_management.repository.TaskAssignmentRepository;
import com.mahmutsalih.task_management.service.UserService;
import com.mahmutsalih.task_management.security.CurrentUserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @BeforeEach
    void setUpAssignmentRepositories() {
        org.mockito.Mockito.lenient()
                .when(projectService.isMember(anyLong(), anyLong()))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(taskAssignmentRepository.findByTaskId(anyLong()))
                .thenReturn(List.of());
    }

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

        when(projectService.getEntityById(1L)).thenReturn(project);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(userService.getEntityById(2L)).thenReturn(user);
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

        when(projectService.getEntityById(1L)).thenReturn(project);
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
        verify(userService, never()).getEntityById(anyLong());
    }

    @Test
    void create_withMultipleProjectMembers_shouldCreateAllAssignments() {
        Project project = Project.builder().id(1L).name("Project").build();
        User firstUser = regularUser(2L, "first@example.com");
        User secondUser = regularUser(3L, "second@example.com");
        TaskRequest request = TaskRequest.builder()
                .title("Shared task")
                .projectId(1L)
                .assigneeIds(List.of(2L, 3L))
                .build();

        when(projectService.getEntityById(1L)).thenReturn(project);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(currentUserService.isAdmin(any(User.class))).thenReturn(true);
        when(userService.getEntityById(2L)).thenReturn(firstUser);
        when(userService.getEntityById(3L)).thenReturn(secondUser);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            return task;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.getAssignees()).extracting("userId").containsExactly(2L, 3L);
        verify(taskAssignmentRepository, times(2)).save(any());
    }

    @Test
    void create_whenAssigneeIsNotProjectMember_shouldRejectTask() {
        Project project = Project.builder().id(1L).name("Project").build();
        User user = regularUser(2L, "user@example.com");
        TaskRequest request = TaskRequest.builder()
                .title("Invalid assignment")
                .projectId(1L)
                .assigneeIds(List.of(2L))
                .build();

        when(projectService.getEntityById(1L)).thenReturn(project);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(currentUserService.isAdmin(any(User.class))).thenReturn(true);
        when(userService.getEntityById(2L)).thenReturn(user);
        when(projectService.isMember(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User is not a member of this project");
        verify(taskRepository, never()).save(any());
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

        when(projectService.getEntityById(1L)).thenReturn(project);
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
        verify(userService, never()).getEntityById(anyLong());
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

        when(projectService.getEntityById(1L)).thenReturn(project);
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

        when(projectService.getEntityById(1L)).thenReturn(project);
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

        when(projectService.getEntityById(1L)).thenReturn(project);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(userService.getEntityById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: " + 99L));

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

        when(projectService.getEntityById(1L)).thenReturn(project);
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
        when(currentUserService.isAdmin()).thenReturn(true);

        TaskResponse response = taskService.getById(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getProjectName()).isEqualTo("Project");
        assertThat(response.getAssignedUsername()).isEqualTo("mahmut@example.com");
        assertThat(response.getAssignedUserFullName()).isEqualTo("Mahmut Kelkit");
    }

    @Test
    void getById_whenCurrentUserIsAssignee_shouldReturnTask() {
        User owner = regularUser(5L, "owner@example.com");
        User assignee = regularUser(2L, "mahmut@example.com");
        Project project = Project.builder().id(1L).name("Project").owner(owner).build();
        Task task = Task.builder()
                .id(3L)
                .title("Assigned task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .assignedUser(assignee)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(assignee);

        TaskResponse response = taskService.getById(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getAssignedUserId()).isEqualTo(2L);
    }

    @Test
    void getById_whenCurrentUserIsNotOwnerOrAssignee_shouldThrowAccessDeniedException() {
        User owner = regularUser(5L, "owner@example.com");
        User assignee = regularUser(2L, "mahmut@example.com");
        User currentUser = regularUser(9L, "other@example.com");
        Project project = Project.builder().id(1L).name("Project").owner(owner).build();
        Task task = Task.builder()
                .id(3L)
                .title("Assigned task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(project)
                .assignedUser(assignee)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> taskService.getById(3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this resource");
    }

    @Test
    void getAll_shouldApplyPaginationSortingAndFilters() {
        Project project = Project.builder().id(1L).name("Project").build();
        Task task = Task.builder()
                .id(3L)
                .title("Create tests")
                .description("Write service tests")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .project(project)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        var response = taskService.getAll(
                2,
                20,
                "dueDate",
                "asc",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                1L,
                4L
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(response.getContent()).hasSize(1);
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("dueDate").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAll_whenSortParamsAreInvalid_shouldUseDefaults() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        taskService.getAll(
                -1,
                0,
                "invalid",
                "sideways",
                null,
                null,
                null,
                null
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAll_whenCurrentUserIsNotAdmin_shouldScopeToOwnedOrAssignedTasks() {
        User currentUser = regularUser(2L, "mahmut@example.com");
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        taskService.getAll(
                0,
                10,
                "createdAt",
                "desc",
                null,
                null,
                null,
                null
        );

        ArgumentCaptor<Specification<Task>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(taskRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<Task> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Join projectJoin = org.mockito.Mockito.mock(Join.class);
        Join assignedUserJoin = org.mockito.Mockito.mock(Join.class);
        Path<Object> ownerPath = org.mockito.Mockito.mock(Path.class);
        Path<Object> ownerIdPath = org.mockito.Mockito.mock(Path.class);
        Path<Object> assignedUserIdPath = org.mockito.Mockito.mock(Path.class);
        Predicate ownerPredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate assigneePredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate visibilityPredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate finalPredicate = org.mockito.Mockito.mock(Predicate.class);

        when(root.join("project", jakarta.persistence.criteria.JoinType.LEFT)).thenReturn(projectJoin);
        when(root.join("assignedUser", jakarta.persistence.criteria.JoinType.LEFT)).thenReturn(assignedUserJoin);
        when(projectJoin.get("owner")).thenReturn(ownerPath);
        when(ownerPath.get("id")).thenReturn(ownerIdPath);
        when(assignedUserJoin.get("id")).thenReturn(assignedUserIdPath);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(criteriaBuilder.equal(ownerIdPath, 2L)).thenReturn(ownerPredicate);
        when(criteriaBuilder.equal(assignedUserIdPath, 2L)).thenReturn(assigneePredicate);
        when(criteriaBuilder.or(ownerPredicate, assigneePredicate)).thenReturn(visibilityPredicate);
        when(criteriaBuilder.and(visibilityPredicate)).thenReturn(finalPredicate);

        Predicate predicate = specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isEqualTo(finalPredicate);
        verify(currentUserService).getCurrentUser();
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
        when(projectService.getEntityById(2L)).thenReturn(project);
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
        when(projectService.getEntityById(1L)).thenReturn(project);

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
        when(projectService.getEntityById(1L)).thenReturn(project);
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
        when(projectService.getEntityById(1L)).thenReturn(project);
        when(userService.getEntityById(4L)).thenReturn(assignee);
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

    @Test
    void create_whenDeadlineIsAfterProjectEndDate_shouldThrowBadRequestException() {
        Project project = Project.builder()
                .id(1L)
                .name("Project")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 10))
                .build();
        TaskRequest request = TaskRequest.builder()
                .title("Late task")
                .dueDate(LocalDate.of(2026, 7, 11))
                .projectId(1L)
                .build();

        when(projectService.getEntityById(1L)).thenReturn(project);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Task deadline must be within the project date range.");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void create_whenDeadlineIsBeforeProjectStartDate_shouldThrowBadRequestException() {
        Project project = Project.builder()
                .id(1L)
                .name("Project")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 10))
                .build();
        TaskRequest request = TaskRequest.builder()
                .title("Early task")
                .dueDate(LocalDate.of(2026, 6, 30))
                .projectId(1L)
                .build();

        when(projectService.getEntityById(1L)).thenReturn(project);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Task deadline must be within the project date range.");
    }

    @Test
    void update_whenDeadlineIsOutsideNewProjectRange_shouldThrowBadRequestException() {
        Project oldProject = Project.builder().id(1L).name("Old Project").build();
        Project newProject = Project.builder()
                .id(2L)
                .name("New Project")
                .endDate(LocalDate.of(2026, 7, 10))
                .build();
        Task task = Task.builder().id(3L).project(oldProject).build();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 7, 11))
                .projectId(2L)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(projectService.getEntityById(2L)).thenReturn(newProject);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        assertThatThrownBy(() -> taskService.update(3L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Task deadline must be within the project date range.");
        verify(taskRepository, never()).save(any(Task.class));
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
