package com.resume.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投递记录实体。
 */
@Data
@TableName(value = "delivery_record")
public class DeliveryRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String resumeId;
    private String company;
    private String position;
    private String channel;
    private String status;
    private LocalDate applyDate;
    private String jdContent;
    private String note;
    private LocalDateTime interviewTime;
    private String interviewLocation;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}