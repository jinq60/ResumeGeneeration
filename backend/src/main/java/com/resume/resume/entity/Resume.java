package com.resume.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.resume.resume.dto.SectionDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历实体。
 */
@Data
@TableName("resume")
public class Resume {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String title;
    private String scene;
    private String targetPosition;
    private String targetIndustry;
    private String templateId;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<SectionDTO> sections;

    private String status;
    private Integer exportCount;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime lastEditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
