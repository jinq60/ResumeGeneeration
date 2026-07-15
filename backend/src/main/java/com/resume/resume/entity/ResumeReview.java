package com.resume.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 简历点评结果实体。
 */
@Data
@TableName("resume_review")
public class ResumeReview {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String resumeId;
    private String userId;

    /**
     * 综合评分，0-100。
     */
    private Integer overallScore;

    /**
     * 分项评分，JSON 对象，例如：{"completeness":85,"structure":80,...}。
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Integer> dimensionScores;

    /**
     * 修改建议，JSON 数组。
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<ResumeReviewSuggestion> suggestions;

    /**
     * 亮点，JSON 数组。
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> highlights;

    /**
     * 目标岗位 JD，点评时可传入用于匹配度评分。
     */
    private String jobDescription;

    /**
     * 使用的模型名称，例如 ERNIE、GPT-4、GLM-4。
     */
    private String modelName;

    /**
     * 模型版本或调用快照标识。
     */
    private String modelVersion;

    /**
     * 状态：success / failed / pending。
     */
    private String status;

    /**
     * 失败时的错误信息。
     */
    private String errorMsg;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
