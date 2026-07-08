package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.enums.ProjectStatus;
import com.mahmutsalih.task_management.enums.ProjectRole;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectMemberRepository;
import com.mahmutsalih.task_management.repository.ProjectRepository;
import com.mahmutsalih.task_management.service.UserService;
import com.mahmutsalih.task_management.security.CurrentUserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void create_shouldCreateProject() {
        User user = User.builder().id(2L).email("faruk@test.com").build();
        ProjectRequest request = ProjectRequest.builder()
                .name("Task Management")
                .description("Project description")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 2, 1))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            project.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return project;
        });

        ProjectResponse response = projectService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Task Management");
        assertThat(response.getDescription()).isEqualTo("Project description");
        verify(projectRepository).save(org.mockito.ArgumentMatchers.argThat(project -> project.getOwner().equals(user)));
        verify(projectMemberRepository).save(org.mockito.ArgumentMatchers.argThat(member ->
                member.getProject().getId().equals(1L)
                        && member.getUser().equals(user)
                        && member.getRole() == ProjectRole.OWNER));
    }

    @Test
    void getAll_whenCurrentUserIsAdmin_shouldReturnAllProjects() {
        Pageable pageable = Pageable.unpaged();
        Project project = Project.builder().id(1L).name("Admin Project").build();

        when(currentUserService.isAdmin()).thenReturn(true);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(project)));

        Page<ProjectResponse> response = projectService.getAll(pageable);

        assertThat(response.getContent()).extracting(ProjectResponse::getName).containsExactly("Admin Project");
        verify(projectRepository).findAll(pageable);
    }

    @Test
    void getAll_whenCurrentUserIsNotAdmin_shouldReturnVisibleProjects() {
        Pageable pageable = Pageable.unpaged();
        User user = User.builder().id(2L).email("user@test.com").build();
        Project project = Project.builder().id(1L).name("Visible Project").owner(user).build();

        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.findVisibleToUser(user, pageable)).thenReturn(new PageImpl<>(List.of(project)));

        Page<ProjectResponse> response = projectService.getAll(pageable);

        assertThat(response.getContent()).extracting(ProjectResponse::getName).containsExactly("Visible Project");
        verify(projectRepository).findVisibleToUser(user, pageable);
    }

    @Test
    void getById_shouldReturnProject() {
        Project project = Project.builder()
                .id(1L)
                .name("Task Management")
                .description("Project description")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.canAccessProject(project)).thenReturn(true);

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Task Management");
        verify(currentUserService).canAccessProject(project);
    }

    @Test
    void getById_whenCurrentUserHasAssignedTaskInProject_shouldReturnProject() {
        User user = User.builder().id(2L).email("user@test.com").build();
        Project project = Project.builder()
                .id(1L)
                .name("Assigned Project")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.canAccessProject(project)).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.existsAssignedTaskInProject(1L, user)).thenReturn(true);

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getName()).isEqualTo("Assigned Project");
    }

    @Test
    void getById_whenCurrentUserCannotAccessProjectAndHasNoAssignedTask_shouldThrowAccessDeniedException() {
        User user = User.builder().id(2L).email("user@test.com").build();
        Project project = Project.builder().id(1L).name("Other Project").build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.canAccessProject(project)).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.existsAssignedTaskInProject(1L, user)).thenReturn(false);

        assertThatThrownBy(() -> projectService.getById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this resource");
    }

    @Test
    void getById_whenProjectNotFound_shouldThrowResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
    }

    @Test
    void getById_whenDeadlineHasPassed_shouldReturnExpiredStatus() {
        Project project = Project.builder()
                .id(1L)
                .name("Expired Project")
                .endDate(LocalDate.now().minusDays(1))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.canAccessProject(project)).thenReturn(true);

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getDeadlineStatus()).isEqualTo(ProjectStatus.EXPIRED);
    }

    @Test
    void getById_whenDeadlineIsTodayOrMissing_shouldReturnActiveStatus() {
        Project project = Project.builder()
                .id(1L)
                .name("Active Project")
                .endDate(LocalDate.now())
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(currentUserService.canAccessProject(project)).thenReturn(true);

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getDeadlineStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void isMember_shouldDelegateToProjectMemberRepository() {
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(true);

        assertThat(projectService.isMember(1L, 2L)).isTrue();

        verify(projectMemberRepository).existsByProjectIdAndUserId(1L, 2L);
    }
}
