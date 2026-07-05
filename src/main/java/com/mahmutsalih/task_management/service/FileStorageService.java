package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.storage.FileStorageResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileStorageResult upload(MultipartFile file, Long taskId);

    byte[] download(String objectKey);

    void delete(String objectKey);
}
