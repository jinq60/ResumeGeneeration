package com.resume.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知数据访问。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}