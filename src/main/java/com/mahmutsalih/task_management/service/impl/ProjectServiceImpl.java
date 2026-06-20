package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.ProjectService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

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

        return toResponse(projectRepository.save(project));
    }

    @Override
    public Page<ProjectResponse> getAll(Pageable pageable) {
        if (currentUserService.isAdmin()) {
            return projectRepository.findAll(pageable)
                    .map(this::toResponse);
        }

        return projectRepository.findByOwner(currentUserService.getCurrentUser(), pageable)
                .map(this::toResponse);
    }

    @Override
    public ProjectResponse getById(Long id) {
        Project project = findProject(id);
        currentUserService.validateProjectAccess(project);
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

        return toResponse(projectRepository.save(project));
    }

    @Override
    public void delete(Long id) {
        Project project = findProject(id);
        currentUserService.validateProjectAccess(project);
        projectRepository.delete(project);
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

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
