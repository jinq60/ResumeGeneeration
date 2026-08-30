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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.util.List;

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
     * 事务提交后再删除对象：若当前处于 Spring 事务中，注册 afterCommit 回调；
     * 否则立即删除（兼容非事务调用）。避免事务回滚后 DB 记录与 MinIO 文件不一致。
     */
    public void removeAfterCommit(String bucket, List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String objectName : objectNames) {
                        remove(bucket, objectName);
                    }
                }
            });
        } else {
            for (String objectName : objectNames) {
                remove(bucket, objectName);
            }
        }
    }

    /**
     * 事务回滚后清理本次上传的新文件，避免事务回滚后 MinIO 孤儿文件。
     */
    public void removeOnRollback(String bucket, String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        remove(bucket, objectName);
                    }
                }
            });
        }
    }

    /**
     * 从指定 Bucket 下载文件（全量内存）。
     * @deprecated 大文件请使用 {@link #downloadStream(String, String)} 流式下载，避免 OOM
     */
    @Deprecated
    public byte[] download(String bucket, String objectName) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build())) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length > 5 * 1024 * 1024) {
                log.warn("Large file download via byte[]: bucket={}, object={}, size={} - consider stream", bucket, objectName, bytes.length);
            }
            return bytes;
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
