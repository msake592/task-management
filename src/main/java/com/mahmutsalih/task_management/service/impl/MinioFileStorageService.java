package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.storage.FileStorageResult;
import com.mahmutsalih.task_management.exception.FileStorageException;
import com.mahmutsalih.task_management.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final String endpoint;
    private final String bucketName;

    public MinioFileStorageService(
            MinioClient minioClient,
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.bucket-name}") String bucketName
    ) {
        this.minioClient = minioClient;
        this.endpoint = endpoint;
        this.bucketName = bucketName;
    }

    @PostConstruct
    void initializeBucket() {
        log.info("Initializing MinIO storage with endpoint '{}'", endpoint);
        log.info("Configured MinIO bucket is '{}'", bucketName);
        log.info("Checking whether MinIO bucket '{}' exists", bucketName);
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (exists) {
                log.info("MinIO bucket '{}' already exists", bucketName);
                return;
            }

            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("MinIO bucket '{}' created successfully", bucketName);
        } catch (Exception exception) {
            log.error(
                    "Failed to initialize MinIO bucket '{}' using endpoint '{}'",
                    bucketName,
                    endpoint,
                    exception
            );
            throw new FileStorageException(
                    "Failed to initialize MinIO bucket '" + bucketName
                            + "' using endpoint '" + endpoint + "'",
                    exception
            );
        }
    }

    @Override
    public FileStorageResult upload(MultipartFile file, Long taskId) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File cannot be empty");
        }
        if (taskId == null) {
            throw new FileStorageException("Task id is required");
        }

        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "-" + sanitizedFileName;
        String objectKey = "tasks/" + taskId + "/" + storedFileName;
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : DEFAULT_CONTENT_TYPE;

        try {
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }

            return FileStorageResult.builder()
                    .storedFileName(storedFileName)
                    .objectKey(objectKey)
                    .bucketName(bucketName)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .build();
        } catch (FileStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileStorageException("File could not be uploaded", exception);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        validateObjectKey(objectKey);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build())) {
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new FileStorageException("File could not be downloaded", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("File could not be deleted", exception);
        }
    }

    private String sanitizeFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new FileStorageException("File name cannot be empty");
        }

        String normalized = originalFileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        String sanitized = baseName
                .replaceAll("[\\p{Cntrl}]", "")
                .replaceAll("[^\\p{L}\\p{N}._ -]", "_")
                .trim();

        if (!StringUtils.hasText(sanitized) || ".".equals(sanitized) || "..".equals(sanitized)) {
            throw new FileStorageException("File name cannot be empty");
        }
        return sanitized;
    }

    private void validateObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new FileStorageException("Object key cannot be empty");
        }
    }
}
