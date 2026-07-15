package com.resume.resume.dto.section;

import lombok.Data;

import java.util.Map;

/**
 * Custom 模块数据。
 */
@Data
public class CustomData {

    /**
     * 自定义模块内容，键值对形式存储。
     * 具体结构由用户自定义，前端渲染时按约定处理。
     */
    private Map<String, Object> content;
}
