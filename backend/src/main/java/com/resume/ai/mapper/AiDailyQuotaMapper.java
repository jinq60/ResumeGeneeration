package com.resume.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.ai.entity.AiDailyQuota;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 每日配额计数 Mapper。
 */
@Mapper
public interface AiDailyQuotaMapper extends BaseMapper<AiDailyQuota> {
}
