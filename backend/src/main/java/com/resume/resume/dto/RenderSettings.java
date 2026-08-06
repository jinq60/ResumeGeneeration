package com.resume.resume.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * 简历渲染设置。模板提供基础样式，用户设置覆盖对应的排版变量。
 */
@Data
@Accessors(chain = true)
public class RenderSettings {

    public static final String DEFAULT_FONT_FAMILY = "\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif";
    public static final String DEFAULT_ACCENT_COLOR = "#1a5276";
    private static final Set<String> ALLOWED_FONT_FAMILIES = Set.of(
            DEFAULT_FONT_FAMILY,
            "Arial, sans-serif",
            "\"Source Han Sans SC\", \"Noto Sans SC\", sans-serif",
            "\"SimSun\", serif"
    );

    private Boolean autoOnePage = false;

    @Size(max = 128, message = "字体设置过长。")
    private String fontFamily;

    @DecimalMin(value = "8.0", message = "字号不能小于 8pt。")
    @DecimalMax(value = "16.0", message = "字号不能大于 16pt。")
    private Double baseFontSize;

    @DecimalMin(value = "1.0", message = "行高不能小于 1。")
    @DecimalMax(value = "2.2", message = "行高不能大于 2.2。")
    private Double lineHeight;

    @DecimalMin(value = "8.0", message = "页面边距不能小于 8mm。")
    @DecimalMax(value = "30.0", message = "页面边距不能大于 30mm。")
    private Double pagePadding;

    @DecimalMin(value = "4.0", message = "模块间距不能小于 4px。")
    @DecimalMax(value = "32.0", message = "模块间距不能大于 32px。")
    private Double sectionSpacing;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "主题色格式不正确。")
    private String accentColor;

    public static RenderSettings defaults() {
        return new RenderSettings();
    }

    /**
     * 复制并清洗外部输入，避免用户设置直接进入 CSS。
     */
    public static RenderSettings sanitized(RenderSettings source) {
        if (source == null) {
            return defaults();
        }
        RenderSettings target = new RenderSettings();
        target.autoOnePage = source.autoOnePage != null && source.autoOnePage;
        target.fontFamily = source.fontFamily == null ? null
                : ALLOWED_FONT_FAMILIES.contains(source.fontFamily) ? source.fontFamily : null;
        target.baseFontSize = clampNullable(source.baseFontSize, 8.0, 16.0);
        target.lineHeight = clampNullable(source.lineHeight, 1.0, 2.2);
        target.pagePadding = clampNullable(source.pagePadding, 8.0, 30.0);
        target.sectionSpacing = clampNullable(source.sectionSpacing, 4.0, 32.0);
        target.accentColor = source.accentColor != null
                && source.accentColor.matches("^#[0-9a-fA-F]{6}$") ? source.accentColor : null;
        return target;
    }

    public static RenderSettings copyOf(RenderSettings source) {
        return sanitized(source);
    }

    private static Double clampNullable(Double value, double min, double max) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Math.max(min, Math.min(max, value));
    }
}
