package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.response.TaskAttachmentResponse;
import com.mahmutsalih.task_management.dto.storage.FileStorageResult;
import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Task;
import com.mahmutsalih.task_management.entity.TaskAttachment;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.TaskAssignmentRepository;
import com.mahmutsalih.task_management.repository.TaskAttachmentRepository;
import com.mahmutsalih.task_management.repository.TaskRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.FileStorageService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    @Test
    void upload_whenValidFile_shouldSaveMetadata() {
        Task task = task();
        User user = user();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
        FileStorageResult storageResult = FileStorageResult.builder()
                .storedFileName("uuid-report.pdf")
                .objectKey("tasks/1/uuid-report.pdf")
                .bucketName("task-attachments")
                .contentType("application/pdf")
                .fileSize(file.getSize())
                .build();

        allowAccess(task, user);
        when(fileStorageService.upload(file, 1L)).thenReturn(storageResult);
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(10L);
            attachment.setUploadedAt(LocalDateTime.of(2026, 7, 5, 12, 0));
            return attachment;
        });

        TaskAttachmentResponse response = taskAttachmentService.upload(1L, file);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOriginalFileName()).isEqualTo("report.pdf");
        assertThat(response.getUploadedById()).isEqualTo(2L);
        assertThat(response.getTaskId()).isEqualTo(1L);
        verify(taskAttachmentRepository).save(any(TaskAttachment.class));
    }

    @Test
    void upload_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "content".getBytes()
        );
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskAttachmentService.upload(99L, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void upload_whenFileIsEmpty_shouldThrowBadRequestException() {
        Task task = task();
        User user = user();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
        allowAccess(task, user);

        assertThatThrownBy(() -> taskAttachmentService.upload(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File cannot be empty");
        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void upload_whenContentTypeNotAllowed_shouldThrowBadRequestException() {
        Task task = task();
        User user = user();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "archive.zip",
                "application/zip",
                "content".getBytes()
        );
        allowAccess(task, user);

        assertThatThrownBy(() -> taskAttachmentService.upload(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File type is not supported");
        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void getAttachments_shouldReturnTaskAttachments() {
        Task task = task();
        User user = user();
        TaskAttachment attachment = attachment(task, user);
        allowAccess(task, user);
        when(taskAttachmentRepository.findByTaskId(1L)).thenReturn(List.of(attachment));

        List<TaskAttachmentResponse> response = taskAttachmentService.getAttachments(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getOriginalFileName()).isEqualTo("report.pdf");
        assertThat(response.get(0).getUploadedByUsername()).isEqualTo("user@example.com");
    }

    @Test
    void download_whenAttachmentNotFoundForTask_shouldThrowResourceNotFoundException() {
        Task task = task();
        User user = user();
        allowAccess(task, user);
        when(taskAttachmentRepository.findByIdAndTaskId(50L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskAttachmentService.download(1L, 50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Attachment not found with id: 50 for task: 1");
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void delete_shouldRemoveFromStorageAndRepository() {
        Task task = task();
        User user = user();
        TaskAttachment attachment = attachment(task, user);
        allowAccess(task, user);
        when(taskAttachmentRepository.findByIdAndTaskId(10L, 1L)).thenReturn(Optional.of(attachment));

        taskAttachmentService.delete(1L, 10L);

        verify(fileStorageService).delete("tasks/1/uuid-report.pdf");
        verify(taskAttachmentRepository).delete(attachment);
    }

    private void allowAccess(Task task, User user) {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isAdmin(user)).thenReturn(true);
    }

    private Task task() {
        User owner = User.builder().id(1L).email("owner@example.com").build();
        Project project = Project.builder().id(1L).name("Project").owner(owner).build();
        return Task.builder().id(1L).title("Task").project(project).build();
    }

    private User user() {
        return User.builder()
                .id(2L)
                .firstName("Test")
                .lastName("User")
                .email("user@example.com")
                .build();
    }

    private TaskAttachment attachment(Task task, User user) {
        return TaskAttachment.builder()
                .id(10L)
                .originalFileName("report.pdf")
                .storedFileName("uuid-report.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .bucketName("task-attachments")
                .objectKey("tasks/1/uuid-report.pdf")
                .uploadedAt(LocalDateTime.of(2026, 7, 5, 12, 0))
                .uploadedBy(user)
                .task(task)
                .build();
    }
}
