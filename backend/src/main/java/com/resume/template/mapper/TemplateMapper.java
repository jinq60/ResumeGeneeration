package com.resume.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.template.entity.Template;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板数据访问层。
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {
}
