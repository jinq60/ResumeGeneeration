package com.resume.resume.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历公开分享实体。
 */
@Data
@TableName("resume_share")
public class ResumeShare {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String resumeId;
    private String userId;
    private String token;
    private String status;
    private Boolean hideContact;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
