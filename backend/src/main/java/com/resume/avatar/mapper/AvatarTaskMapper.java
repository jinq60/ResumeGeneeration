package com.resume.avatar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.avatar.entity.AvatarTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 头像任务数据访问层。
 */
@Mapper
public interface AvatarTaskMapper extends BaseMapper<AvatarTask> {
}
