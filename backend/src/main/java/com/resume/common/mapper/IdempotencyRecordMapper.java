package com.resume.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.common.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {
}
