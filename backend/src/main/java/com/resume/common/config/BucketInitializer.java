package com.resume.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动时确保 MinIO Bucket 存在。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void initBuckets() {
        ensureBucket(minioConfig.getBuckets().getAvatars());
        ensureBucket(minioConfig.getBuckets().getPdfs());
        ensureBucket(minioConfig.getBuckets().getTemplates());
    }

    private void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Failed to ensure MinIO bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
