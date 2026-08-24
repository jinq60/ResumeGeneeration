package com.resume.notification.dto;

import com.resume.notification.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应。
 */
@Data
public class NotificationResponse {

    private String id;
    private String type;
    private String title;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRead(Integer.valueOf(1).equals(notification.getReadFlag()));
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}