package com.resume.delivery.dto;

import com.resume.delivery.entity.DeliveryRecord;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投递记录响应。
 */
@Data
public class DeliveryRecordResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeliveryRecordResponse from(DeliveryRecord record) {
        DeliveryRecordResponse response = new DeliveryRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setResumeId(record.getResumeId());
        response.setCompany(record.getCompany());
        response.setPosition(record.getPosition());
        response.setChannel(record.getChannel());
        response.setStatus(record.getStatus());
        response.setApplyDate(record.getApplyDate());
        response.setJdContent(record.getJdContent());
        response.setNote(record.getNote());
        response.setInterviewTime(record.getInterviewTime());
        response.setInterviewLocation(record.getInterviewLocation());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }
}