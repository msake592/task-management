package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.request.CommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;
import com.mahmutsalih.task_management.entity.Comment;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.enums.TaskPriority;
import com.mahmutsalih.task_management.enums.TaskStatus;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.CommentRepository;
import com.mahmutsalih.task_management.repository.TaskRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void addCommentToTask_shouldCreateComment() {
        Task task = task();
        User user = user();
        CommentRequest request = CommentRequest.builder()
                .content("Looks good")
                .userId(2L)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(4L);
            comment.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
            return comment;
        });

        CommentResponse response = commentService.addCommentToTask(3L, request);

        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getContent()).isEqualTo("Looks good");
        assertThat(response.getTaskId()).isEqualTo(3L);
        assertThat(response.getUserId()).isEqualTo(2L);
    }

    @Test
    void getCommentsByTaskId_shouldReturnComments() {
        Task task = task();
        User user = user();
        Comment comment = Comment.builder()
                .id(4L)
                .content("Looks good")
                .task(task)
                .user(user)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(commentRepository.findByTaskId(3L)).thenReturn(List.of(comment));

        List<CommentResponse> responses = commentService.getCommentsByTaskId(3L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getContent()).isEqualTo("Looks good");
        assertThat(responses.get(0).getTaskTitle()).isEqualTo("Create tests");
    }

    @Test
    void getCommentsByTaskId_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsByTaskId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
    }

    @Test
    void delete_whenCommentNotFound_shouldThrowResourceNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found with id: 99");
    }

    private Task task() {
        return Task.builder()
                .id(3L)
                .title("Create tests")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .project(Project.builder().id(1L).name("Project").build())
                .build();
    }

    private User user() {
        return User.builder()
                .id(2L)
                .firstName("Mahmut")
                .lastName("Kelkit")
                .build();
    }
}
