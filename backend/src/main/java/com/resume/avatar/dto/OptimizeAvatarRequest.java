package com.resume.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 一寸照优化请求。
 */
@Data
public class OptimizeAvatarRequest {

    @NotBlank(message = "原图地址为必填项。")
    @Size(max = 512, message = "原图地址过长。")
    private String sourceImageUrl;

    @Size(max = 64, message = "简历 ID 过长。")
    private String resumeId;

    @NotBlank(message = "背景类型为必填项。")
    @Pattern(regexp = "^(white|blue|red)$", message = "背景类型不正确，P0 仅支持 white/blue/red。")
    private String backgroundType;

    @NotBlank(message = "照片风格为必填项。")
    @Pattern(regexp = "^(formal|natural|professional)$", message = "照片风格不正确。")
    private String style;

    private Boolean keepIdentity;
    private Boolean enhanceQuality;
    private Boolean removeBackground;
    private Boolean brightenSkin;
}
