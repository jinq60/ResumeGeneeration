package com.resume.pdf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 导出任务实体。
 */
@Data
@TableName("pdf_task")
public class PdfTask {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String resumeId;
    private String templateId;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String status;
    private String errorMsg;
    private LocalDateTime completedAt;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
