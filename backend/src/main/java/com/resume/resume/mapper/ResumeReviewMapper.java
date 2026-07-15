package com.resume.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.resume.entity.ResumeReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 简历点评数据访问层。
 */
@Mapper
public interface ResumeReviewMapper extends BaseMapper<ResumeReview> {
}
