package com.resume.common.service;

import com.resume.common.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 对象存储服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件到指定 Bucket。
     */
    public void upload(String bucket, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucket(bucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            log.error("Upload file to MinIO failed: bucket={}, object={}", bucket, objectName, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除文件。
     */
    public void remove(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.warn("Remove file from MinIO failed: bucket={}, object={}", bucket, objectName, e);
        }
    }

    /**
     * 从指定 Bucket 下载文件。
     */
    public byte[] download(String bucket, String objectName) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("Download file from MinIO failed: bucket={}, object={}", bucket, objectName, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    public String getBucketAvatars() {
        return minioConfig.getBuckets().getAvatars();
    }

    public String getBucketPdfs() {
        return minioConfig.getBuckets().getPdfs();
    }

    public String getBucketTemplates() {
        return minioConfig.getBuckets().getTemplates();
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
