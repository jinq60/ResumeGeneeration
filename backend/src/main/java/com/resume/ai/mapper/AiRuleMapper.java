package com.resume.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.ai.entity.AiRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 规则数据访问。
 */
@Mapper
public interface AiRuleMapper extends BaseMapper<AiRule> {
}