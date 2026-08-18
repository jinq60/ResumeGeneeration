package com.resume.resume.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Section 统一结构。
 */
@Data
public class SectionDTO {

    @NotBlank(message = "模块 ID 为必填项。")
    private String id;

    @NotBlank(message = "模块类型为必填项。")
    private String type;

    @NotBlank(message = "模块标题为必填项。")
    private String title;

    @NotNull(message = "模块排序为必填项。")
    private Integer order;

    @NotNull(message = "模块展示状态为必填项。")
    private Boolean visible;

    @NotNull(message = "模块数据不能为空。")
    @Valid
    private Object data;

    /**
     * 以 Map 形式访问 data（profile / introduction / custom 等对象型模块）。
     * data 非 Map 时返回空 Map，避免调用方散落 instanceof 强转。
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Map<String, Object> dataAsMap() {
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return Map.of();
    }

    /**
     * 以列表形式访问 data（education / work / project / skill 等数组型模块）。
     * 过滤非 Map 元素，data 非 List 时返回空列表，避免调用方散落强转。
     */
    @JsonIgnore
    public List<Map<String, Object>> dataAsItems() {
        if (!(data instanceof List<?> raw)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                result.add(map);
            }
        }
        return result;
    }
}
