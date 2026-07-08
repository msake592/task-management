package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.AddProjectMemberRequest;
import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectMemberResponse;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.ProjectMember;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.enums.ProjectRole;
import com.mahmutsalih.task_management.enums.ProjectStatus;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.ProjectMemberRepository;
import com.mahmutsalih.task_management.repository.ProjectRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.ProjectService;
import com.mahmutsalih.task_management.service.UserService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
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
        projectMemberRepository.save(ProjectMember.builder()
                .project(savedProject)
                .user(currentUser)
                .role(ProjectRole.OWNER)
                .build());
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
    public Project getEntityById(Long id) {
        return findProject(id);
    }

    @Override
    public boolean isMember(Long projectId, Long userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
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

    @Override
    @Transactional
    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request) {
        Project project = findProject(projectId);
        validateMemberManagementAccess(project);
        User user = userService.getEntityById(request.getUserId());
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new BadRequestException("User is already a member of this project");
        }

        ProjectMember member = projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(user)
                .role(request.getRole() != null ? request.getRole() : ProjectRole.MEMBER)
                .build());
        return toMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long projectId) {
        Project project = findProject(projectId);
        validateMemberReadAccess(project);
        return projectMemberRepository.findByProjectIdWithUser(projectId).stream()
                .map(this::toMemberResponse)
                .toList();
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
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId())) {
            return;
        }
        if (projectRepository.existsAssignedTaskInProject(project.getId(), currentUser)) {
            return;
        }

        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    private void validateMemberManagementAccess(Project project) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User currentUser = currentUserService.getCurrentUser();
        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED_MESSAGE));
        if (membership.getRole() != ProjectRole.OWNER && membership.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    private void validateMemberReadAccess(Project project) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User currentUser = currentUserService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId())) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        User user = member.getUser();
        return ProjectMemberResponse.builder()
                .userId(user.getId())
                .name(getFullName(user))
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private String getFullName(User user) {
        return user.getLastName() == null || user.getLastName().isBlank()
                ? user.getFirstName()
                : user.getFirstName() + " " + user.getLastName();
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
