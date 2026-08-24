package com.resume.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建 / 更新投递记录请求。
 */
@Data
public class DeliveryRecordRequest {

    @NotBlank(message = "请选择关联简历")
    private String resumeId;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 128, message = "公司名称最长 128 字符")
    private String company;

    @NotBlank(message = "职位名称不能为空")
    @Size(max = 128, message = "职位名称最长 128 字符")
    private String position;

    @Size(max = 32, message = "投递渠道最长 32 字符")
    private String channel;

    @Size(max = 32, message = "状态值不合法")
    private String status;

    private LocalDate applyDate;

    @Size(max = 5000, message = "JD 内容过长")
    private String jdContent;

    @Size(max = 512, message = "备注最长 512 字符")
    private String note;

    private LocalDateTime interviewTime;

    @Size(max = 255, message = "面试地点最长 255 字符")
    private String interviewLocation;
}