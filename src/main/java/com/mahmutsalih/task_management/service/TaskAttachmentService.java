package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.response.TaskAttachmentDownloadResponse;
import com.mahmutsalih.task_management.dto.response.TaskAttachmentResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TaskAttachmentService {

    TaskAttachmentResponse upload(Long taskId, MultipartFile file);

    List<TaskAttachmentResponse> getAttachments(Long taskId);

    TaskAttachmentDownloadResponse download(Long taskId, Long attachmentId);

    void delete(Long taskId, Long attachmentId);
}
