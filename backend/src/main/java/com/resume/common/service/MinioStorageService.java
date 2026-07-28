package com.resume.common.service;

import com.resume.common.config.MinioConfig;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
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
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            log.error("Upload file to MinIO failed: bucket={}, object={}", bucket, objectName, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败，请稍后重试。", e);
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
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件下载失败，请稍后重试。", e);
        }
    }

    /**
     * 以流方式获取对象，调用方负责关闭 InputStream。
     * <p>
     * 用于大文件（如 PDF）下载，避免一次性读入内存导致 OOM。
     * </p>
     */
    public InputStream downloadStream(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.error("Stream file from MinIO failed: bucket={}, object={}", bucket, objectName, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件下载失败，请稍后重试。", e);
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
}
