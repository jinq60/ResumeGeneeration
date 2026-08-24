package com.resume.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.delivery.entity.DeliveryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投递记录数据访问。
 */
@Mapper
public interface DeliveryRecordMapper extends BaseMapper<DeliveryRecord> {
}