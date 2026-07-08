package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.CreateCommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;
import com.mahmutsalih.task_management.entity.Comment;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.CommentRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.CommentService;
import com.mahmutsalih.task_management.service.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    @Override
    public CommentResponse addComment(Long taskId, CreateCommentRequest request) {
        Task task = findTask(taskId);
        User currentUser = getCurrentUser();

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .build();
        comment.setUser(currentUser);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment added. commentId={}, taskId={}, userId={}", savedComment.getId(), taskId, currentUser.getId());
        return toResponse(savedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByTask(Long taskId) {
        findTask(taskId);

        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Task findTask(Long id) {
        return taskService.getEntityById(id);
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private CommentResponse toResponse(Comment comment) {
        Task task = comment.getTask();
        User user = comment.getUser();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .taskId(task.getId())
                .userId(user.getId())
                .username(user.getEmail())
                .build();
    }
}
