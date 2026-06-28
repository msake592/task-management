package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.enums.ProjectStatus;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.ProjectService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";

    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Override
    public ProjectResponse create(ProjectRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        User currentUser = currentUserService.getCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .owner(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created. projectId={}, name={}", savedProject.getId(), savedProject.getName());
        return toResponse(savedProject);
    }

    @Override
    public Page<ProjectResponse> getAll(Pageable pageable) {
        if (currentUserService.isAdmin()) {
            return projectRepository.findAll(pageable)
                    .map(this::toResponse);
        }

        return projectRepository.findVisibleToUser(currentUserService.getCurrentUser(), pageable)
                .map(this::toResponse);
    }

    @Override
    public ProjectResponse getById(Long id) {
        Project project = findProject(id);
        validateProjectReadAccess(project);
        return toResponse(project);
    }

    @Override
    public ProjectResponse update(Long id, ProjectRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Project project = findProject(id);
        currentUserService.validateProjectAccess(project);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        Project savedProject = projectRepository.save(project);
        log.info("Project updated. projectId={}", id);
        return toResponse(savedProject);
    }

    @Override
    public void delete(Long id) {
        Project project = findProject(id);
        currentUserService.validateProjectAccess(project);
        projectRepository.delete(project);
        log.info("Project deleted. projectId={}", id);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private void validateProjectReadAccess(Project project) {
        if (currentUserService.canAccessProject(project)) {
            return;
        }

        User currentUser = currentUserService.getCurrentUser();
        if (projectRepository.existsAssignedTaskInProject(project.getId(), currentUser)) {
            return;
        }

        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .deadlineStatus(resolveDeadlineStatus(project.getEndDate()))
                .build();
    }

    private ProjectStatus resolveDeadlineStatus(LocalDate endDate) {
        return endDate != null && endDate.isBefore(LocalDate.now())
                ? ProjectStatus.EXPIRED
                : ProjectStatus.ACTIVE;
    }
}
