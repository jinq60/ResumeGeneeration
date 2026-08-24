package com.resume.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import com.resume.resume.dto.SectionDTO;
import com.resume.resume.dto.RenderSettings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历实体。
 */
@Data
@TableName(value = "resume", autoResultMap = true)
public class Resume {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String title;
    private String scene;
    private String targetPosition;
    private String targetIndustry;
    private String templateId;

    @TableField(typeHandler = com.resume.resume.handler.SectionListTypeHandler.class)
    private List<SectionDTO> sections;

    @TableField(typeHandler = com.resume.resume.handler.RenderSettingsTypeHandler.class)
    private RenderSettings renderSettings;

    private String status;
    private Integer exportCount;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    /**
     * 乐观锁版本号：整行更新时自动校验与自增，防止自动保存并发丢失更新。
     */
    @Version
    private Integer version;

    private LocalDateTime lastEditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
