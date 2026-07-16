package com.resume.common.config;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Buckets buckets = new Buckets();
    private int presignedUrlExpiration = 3600;

    @Data
    public static class Buckets {
        private String avatars;
        private String pdfs;
        private String templates;
    }

    @PostConstruct
    public void validateBuckets() {
        if (buckets == null
                || isBlank(buckets.getAvatars())
                || isBlank(buckets.getPdfs())
                || isBlank(buckets.getTemplates())) {
            throw new IllegalStateException("MinIO bucket names must be configured via app.minio.buckets.*");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
