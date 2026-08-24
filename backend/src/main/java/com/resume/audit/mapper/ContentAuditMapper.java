package com.resume.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.audit.entity.ContentAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容审核记录数据访问。
 */
@Mapper
public interface ContentAuditMapper extends BaseMapper<ContentAudit> {
}