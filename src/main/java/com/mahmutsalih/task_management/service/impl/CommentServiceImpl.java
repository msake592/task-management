package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.CommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;
import com.mahmutsalih.task_management.entity.Comment;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.CommentRepository;
import com.mahmutsalih.task_management.repository.TaskRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.CommentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    @Override
    public CommentResponse addCommentToTask(Long taskId, CommentRequest request) {
        Task task = findTask(taskId);
        currentUserService.validateProjectAccess(task.getProject());
        User user = currentUserService.getCurrentUser();

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .user(user)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    @Override
    public List<CommentResponse> getCommentsByTaskId(Long taskId) {
        Task task = findTask(taskId);
        currentUserService.validateProjectAccess(task.getProject());

        return commentRepository.findByTaskId(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CommentResponse update(Long id, CommentRequest request) {
        Comment comment = findComment(id);
        validateCommentManageAccess(comment);
        comment.setContent(request.getContent());
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public void delete(Long id) {
        Comment comment = findComment(id);
        validateCommentManageAccess(comment);
        commentRepository.delete(comment);
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private void validateCommentManageAccess(Comment comment) {
        if (currentUserService.isAdmin() || currentUserService.canAccessProject(comment.getTask().getProject())) {
            return;
        }

        User currentUser = currentUserService.getCurrentUser();
        User commentUser = comment.getUser();
        if (commentUser != null && commentUser.getId() != null && commentUser.getId().equals(currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to access this resource");
    }

    private CommentResponse toResponse(Comment comment) {
        Task task = comment.getTask();
        User user = comment.getUser();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .userId(user.getId())
                .userFullName(getFullName(user))
                .build();
    }

    private String getFullName(User user) {
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            return user.getFirstName();
        }

        return user.getFirstName() + " " + user.getLastName();
    }
}
