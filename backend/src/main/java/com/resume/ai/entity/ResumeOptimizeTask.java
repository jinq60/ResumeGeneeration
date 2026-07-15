package com.resume.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.resume.ai.dto.ResumeOptimizeResponse.SectionOptimization;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * JD 匹配优化任务实体。
 */
@Data
@TableName("resume_optimize_task")
public class ResumeOptimizeTask {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String resumeId;
    private String userId;
    private String jobDescription;

    /** JD 匹配评分 0-100 */
    private Integer matchScore;

    /** 分项评分 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Integer> dimensionScores;

    /** 逐模块优化建议 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<SectionOptimization> optimizations;

    /** 缺失技能 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> missingSkills;

    /** 整体建议 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> recommendations;

    private String modelName;
    private String status;
    private String errorMsg;
    private LocalDateTime completedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
