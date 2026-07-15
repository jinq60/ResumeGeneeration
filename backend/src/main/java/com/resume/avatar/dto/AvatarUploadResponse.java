package com.resume.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上传头像响应。
 */
@Data
public class AvatarUploadResponse {

    private String id;
    private String sourceImageUrl;
    private String fileName;
}
