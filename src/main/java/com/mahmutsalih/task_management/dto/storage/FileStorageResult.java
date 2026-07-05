package com.mahmutsalih.task_management.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStorageResult {

    private String storedFileName;
    private String objectKey;
    private String bucketName;
    private String contentType;
    private Long fileSize;
}
