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
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectRepository;
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
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

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

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Task Management");
        verify(currentUserService).validateProjectAccess(project);
    }

    @Test
    void getById_whenProjectNotFound_shouldThrowResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
    }
}
