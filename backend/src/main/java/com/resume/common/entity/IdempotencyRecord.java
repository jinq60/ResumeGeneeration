package com.resume.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("idempotency_record")
public class IdempotencyRecord {

    @TableId
    private String idempotencyKey;

    private String userId;
    private String httpMethod;
    private String requestPath;
    private Integer responseStatus;
    private String responseBody;
    private String responseContentType;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
