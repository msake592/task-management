package com.mahmutsalih.task_management.controller;

import com.mahmutsalih.task_management.dto.response.TaskAttachmentDownloadResponse;
import com.mahmutsalih.task_management.dto.response.TaskAttachmentResponse;
import com.mahmutsalih.task_management.service.TaskAttachmentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks/{taskId}/attachments")
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    @PostMapping
    public ResponseEntity<TaskAttachmentResponse> upload(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskAttachmentService.upload(taskId, file));
    }

    @GetMapping
    public ResponseEntity<List<TaskAttachmentResponse>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskAttachmentService.getAttachments(taskId));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId
    ) {
        TaskAttachmentDownloadResponse download = taskAttachmentService.download(taskId, attachmentId);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.getResource());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId
    ) {
        taskAttachmentService.delete(taskId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
