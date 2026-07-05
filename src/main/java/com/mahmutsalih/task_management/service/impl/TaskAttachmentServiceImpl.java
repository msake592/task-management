package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.response.TaskAttachmentDownloadResponse;
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
import com.mahmutsalih.task_management.service.TaskAttachmentService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TaskAttachmentResponse upload(Long taskId, MultipartFile file) {
        Task task = findTask(taskId);
        User currentUser = currentUserService.getCurrentUser();
        validateTaskAccess(task, currentUser);
        validateFile(file);

        FileStorageResult storageResult = fileStorageService.upload(file, taskId);
        TaskAttachment attachment = TaskAttachment.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storageResult.getStoredFileName())
                .contentType(storageResult.getContentType())
                .fileSize(storageResult.getFileSize())
                .bucketName(storageResult.getBucketName())
                .objectKey(storageResult.getObjectKey())
                .uploadedBy(currentUser)
                .task(task)
                .build();

        return toResponse(taskAttachmentRepository.save(attachment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getAttachments(Long taskId) {
        Task task = findTask(taskId);
        User currentUser = currentUserService.getCurrentUser();
        validateTaskAccess(task, currentUser);
        return taskAttachmentRepository.findByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAttachmentDownloadResponse download(Long taskId, Long attachmentId) {
        Task task = findTask(taskId);
        User currentUser = currentUserService.getCurrentUser();
        validateTaskAccess(task, currentUser);
        TaskAttachment attachment = findAttachment(taskId, attachmentId);
        byte[] content = fileStorageService.download(attachment.getObjectKey());

        return TaskAttachmentDownloadResponse.builder()
                .originalFileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .resource(new InputStreamResource(new ByteArrayInputStream(content)))
                .build();
    }

    @Override
    @Transactional
    public void delete(Long taskId, Long attachmentId) {
        Task task = findTask(taskId);
        User currentUser = currentUserService.getCurrentUser();
        validateTaskAccess(task, currentUser);
        TaskAttachment attachment = findAttachment(taskId, attachmentId);
        fileStorageService.delete(attachment.getObjectKey());
        taskAttachmentRepository.delete(attachment);
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    private TaskAttachment findAttachment(Long taskId, Long attachmentId) {
        return taskAttachmentRepository.findByIdAndTaskId(attachmentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with id: " + attachmentId + " for task: " + taskId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("File type is not supported");
        }
    }

    private void validateTaskAccess(Task task, User currentUser) {
        if (currentUserService.isAdmin(currentUser)
                || isProjectOwner(task.getProject(), currentUser)
                || isSameUser(task.getAssignedUser(), currentUser)
                || taskAssignmentRepository.existsByTaskIdAndUserId(task.getId(), currentUser.getId())) {
            return;
        }
        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    private boolean isProjectOwner(Project project, User user) {
        User owner = project != null ? project.getOwner() : null;
        return isSameUser(owner, user);
    }

    private boolean isSameUser(User first, User second) {
        return first != null
                && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }

    private TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        User uploadedBy = attachment.getUploadedBy();
        return TaskAttachmentResponse.builder()
                .id(attachment.getId())
                .originalFileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .uploadedAt(attachment.getUploadedAt())
                .uploadedById(uploadedBy.getId())
                .uploadedByUsername(uploadedBy.getEmail())
                .taskId(attachment.getTask().getId())
                .build();
    }
}
