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
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.TaskService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private static final String SELF_ASSIGN_ONLY_MESSAGE = "Users can only assign tasks to themselves.";
    private static final String ASSIGNEE_CHANGE_DENIED_MESSAGE = "Users cannot change task assignee.";
    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "priority", "dueDate", "createdAt", "updatedAt"
    );

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    public TaskResponse create(TaskRequest request) {
        Project project = findProject(request.getProjectId());
        currentUserService.validateProjectAccess(project);
        User currentUser = currentUserService.getCurrentUser();
        boolean admin = currentUserService.isAdmin(currentUser);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .project(project)
                .assignedUser(resolveAssigneeForCreate(request.getAssignedUserId(), currentUser, admin))
                .build();

        Task savedTask = taskRepository.save(task);
        log.info(
                "Task created. taskId={}, assignedUserId={}",
                savedTask.getId(),
                getUserId(savedTask.getAssignedUser())
        );
        return toResponse(savedTask);
    }

    @Override
    public TaskResponse getById(Long id) {
        Task task = findTask(id);
        validateTaskReadAccess(task);
        return toResponse(task);
    }

    @Override
    public Page<TaskResponse> getAll(
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            TaskPriority priority,
            Long projectId,
            Long assignedUserId
    ) {
        log.debug(
                "Task list requested. page={}, size={}, sortBy={}, direction={}, status={}, priority={}, projectId={}, assignedUserId={}",
                page,
                size,
                sortBy,
                direction,
                status,
                priority,
                projectId,
                assignedUserId
        );
        return taskRepository.findAll(
                        buildSpecification(status, priority, projectId, assignedUserId),
                        buildPageable(page, size, sortBy, direction)
                )
                .map(this::toResponse);
    }

    @Override
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = findTask(id);
        validateTaskAccess(task);
        User currentUser = currentUserService.getCurrentUser();
        boolean admin = currentUserService.isAdmin(currentUser);

        Project project = findProject(request.getProjectId());
        currentUserService.validateProjectAccess(project);
        TaskStatus oldStatus = task.getStatus();
        Long oldAssignedUserId = getUserId(task.getAssignedUser());

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setProject(project);
        applyAssigneeForUpdate(task, request.getAssignedUserId(), admin);
        task.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);
        log.info("Task updated. taskId={}", savedTask.getId());
        logStatusChange(savedTask.getId(), oldStatus, savedTask.getStatus());
        logAssignmentChange(savedTask.getId(), oldAssignedUserId, getUserId(savedTask.getAssignedUser()));
        return toResponse(savedTask);
    }

    @Override
    public void delete(Long id) {
        Task task = findTask(id);
        validateTaskAccess(task);
        taskRepository.delete(task);
        log.info("Task deleted. taskId={}", id);
    }

    @Override
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = findTask(id);
        validateTaskAccess(task);
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        logStatusChange(savedTask.getId(), oldStatus, savedTask.getStatus());
        return toResponse(savedTask);
    }

    @Override
    public TaskResponse assignToUser(Long id, Long userId) {
        Task task = findTask(id);
        validateTaskAccess(task);
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException(ASSIGNEE_CHANGE_DENIED_MESSAGE);
        }
        Long oldAssignedUserId = getUserId(task.getAssignedUser());
        task.setAssignedUser(findUser(userId));
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        logAssignmentChange(savedTask.getId(), oldAssignedUserId, getUserId(savedTask.getAssignedUser()));
        return toResponse(savedTask);
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

    private User resolveAssigneeForCreate(Long requestedAssigneeId, User currentUser, boolean admin) {
        if (admin) {
            return findUserOrNull(requestedAssigneeId);
        }

        if (requestedAssigneeId == null || requestedAssigneeId.equals(currentUser.getId())) {
            return currentUser;
        }

        throw new AccessDeniedException(SELF_ASSIGN_ONLY_MESSAGE);
    }

    private void applyAssigneeForUpdate(Task task, Long requestedAssigneeId, boolean admin) {
        if (admin) {
            task.setAssignedUser(findUserOrNull(requestedAssigneeId));
            return;
        }

        User currentAssignee = task.getAssignedUser();
        if (requestedAssigneeId == null || isSameUser(currentAssignee, requestedAssigneeId)) {
            return;
        }

        throw new AccessDeniedException(ASSIGNEE_CHANGE_DENIED_MESSAGE);
    }

    private boolean isSameUser(User user, Long userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    private Long getUserId(User user) {
        return user != null ? user.getId() : null;
    }

    private void logStatusChange(Long taskId, TaskStatus oldStatus, TaskStatus newStatus) {
        if (oldStatus != newStatus) {
            log.info("Task status changed. taskId={}, oldStatus={}, newStatus={}", taskId, oldStatus, newStatus);
        }
    }

    private void logAssignmentChange(Long taskId, Long oldAssignedUserId, Long newAssignedUserId) {
        if (!java.util.Objects.equals(oldAssignedUserId, newAssignedUserId)) {
            log.info(
                    "Task assignment changed. taskId={}, oldAssignedUserId={}, newAssignedUserId={}",
                    taskId,
                    oldAssignedUserId,
                    newAssignedUserId
            );
        }
    }

    private void validateTaskAccess(Task task) {
        currentUserService.validateProjectAccess(task.getProject());
    }

    private void validateTaskReadAccess(Task task) {
        if (currentUserService.isAdmin()) {
            return;
        }

        User currentUser = currentUserService.getCurrentUser();
        if (isProjectOwner(task.getProject(), currentUser) || isSameUser(task.getAssignedUser(), currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    private boolean isProjectOwner(Project project, User user) {
        User owner = project != null ? project.getOwner() : null;
        return owner != null && user != null && owner.getId() != null && owner.getId().equals(user.getId());
    }

    private Specification<Task> buildSpecification(
            TaskStatus status,
            TaskPriority priority,
            Long projectId,
            Long assignedUserId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            var projectJoin = root.join("project", JoinType.LEFT);
            var assignedUserJoin = root.join("assignedUser", JoinType.LEFT);

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            if (projectId != null) {
                predicates.add(criteriaBuilder.equal(projectJoin.get("id"), projectId));
            }

            if (assignedUserId != null) {
                predicates.add(criteriaBuilder.equal(assignedUserJoin.get("id"), assignedUserId));
            }

            if (!currentUserService.isAdmin()) {
                Long currentUserId = currentUserService.getCurrentUser().getId();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(projectJoin.get("owner").get("id"), currentUserId),
                        criteriaBuilder.equal(assignedUserJoin.get("id"), currentUserId)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        int normalizedPage = Math.max(page, DEFAULT_PAGE);
        int normalizedSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String normalizedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction normalizedDirection = Sort.Direction.fromOptionalString(direction)
                .orElse(DEFAULT_SORT_DIRECTION);

        return PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(normalizedDirection, normalizedSortBy)
        );
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
                .updatedAt(task.getUpdatedAt())
                .projectId(project.getId())
                .projectName(project.getName())
                .assignedUserId(assignedUser != null ? assignedUser.getId() : null)
                .assignedUsername(assignedUser != null ? assignedUser.getEmail() : null)
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
