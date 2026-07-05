package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.exception.FileStorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    private static final String BUCKET_NAME = "task-attachments";
    private static final String ENDPOINT = "http://localhost:9000";

    @Mock
    private MinioClient minioClient;

    private MinioFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MinioFileStorageService(minioClient, ENDPOINT, BUCKET_NAME);
    }

    @Test
    void initializeBucket_whenBucketExists_shouldNotCreateIt() throws Exception {
        when(minioClient.bucketExists(bucketArgs())).thenReturn(true);

        storageService.initializeBucket();

        verify(minioClient, never()).makeBucket(org.mockito.ArgumentMatchers.any(MakeBucketArgs.class));
    }

    @Test
    void initializeBucket_whenBucketDoesNotExist_shouldCreateIt() throws Exception {
        when(minioClient.bucketExists(bucketArgs())).thenReturn(false);

        storageService.initializeBucket();

        verify(minioClient).makeBucket(argThat(args -> BUCKET_NAME.equals(args.bucket())));
    }

    @Test
    void initializeBucket_whenMinioCallFails_shouldFailFast() throws Exception {
        RuntimeException cause = new RuntimeException("MinIO unavailable");
        when(minioClient.bucketExists(bucketArgs())).thenThrow(cause);

        assertThatThrownBy(storageService::initializeBucket)
                .isInstanceOf(FileStorageException.class)
                .hasMessage(
                        "Failed to initialize MinIO bucket 'task-attachments' "
                                + "using endpoint 'http://localhost:9000'"
                )
                .hasCause(cause);
    }

    private BucketExistsArgs bucketArgs() {
        return argThat(args -> BUCKET_NAME.equals(args.bucket()));
    }
}
