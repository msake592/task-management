package com.mahmutsalih.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.InputStreamResource;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachmentDownloadResponse {

    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private InputStreamResource resource;
}
