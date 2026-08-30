package com.resume.common.config;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置。
 * <p>
 * 缺省会在本地开发环境（无 MinIO）时打印 WARN 日志但不起容器失败。
 * 通过 {@code app.minio.endpoint} 配置启用，未配置或为空时跳过 MinioClient 创建，
 * 由 {@link BucketInitializer} 与 {@code MinioStorageService} 自动捕获并软失败。
 * </p>
 */
@Slf4j
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
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "MinIO endpoint must be configured via app.minio.endpoint (env: APP_MINIO_ENDPOINT). "
                            + "Local dev should set it in application-dev.yml (e.g. http://localhost:9000).");
        }
        // 校验 URL 合法性，避免 ftp:// 等非法协议在运行时才暴露
        try {
            java.net.URI uri = java.net.URI.create(endpoint.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalStateException("MinIO endpoint must be http(s) URL: " + endpoint);
            }
            if (uri.getHost() == null) {
                throw new IllegalStateException("MinIO endpoint must contain host: " + endpoint);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid MinIO endpoint: " + endpoint, e);
        }
        // 凭据缺失必须显式报错，禁止静默回退到众所周知的 minioadmin/minioadmin 掩盖配置错误；
        // 本地开发可在 application-dev.yml 中显式声明默认值
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "MinIO access-key/secret-key must be configured via app.minio.access-key / app.minio.secret-key "
                            + "(env: APP_MINIO_ACCESS_KEY / APP_MINIO_SECRET_KEY)");
        }
        log.info("MinIO client configured: endpoint={}", endpoint);
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
