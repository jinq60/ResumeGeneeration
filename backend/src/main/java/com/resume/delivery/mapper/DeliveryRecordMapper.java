package com.resume.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.delivery.entity.DeliveryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投递记录数据访问。
 */
@Mapper
public interface DeliveryRecordMapper extends BaseMapper<DeliveryRecord> {

    @org.apache.ibatis.annotations.Select("SELECT status, COUNT(*) as cnt FROM delivery_record WHERE deleted = 0 GROUP BY status")
    @org.apache.ibatis.annotations.MapKey("status")
    java.util.List<java.util.Map<String, Object>> countByStatus();

    @org.apache.ibatis.annotations.Select("SELECT position, COUNT(*) as cnt FROM delivery_record WHERE deleted = 0 AND position IS NOT NULL AND position <> '' GROUP BY position ORDER BY cnt DESC LIMIT 5")
    java.util.List<java.util.Map<String, Object>> topPositions();

    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM delivery_record WHERE deleted = 0")
    long countActive();
}