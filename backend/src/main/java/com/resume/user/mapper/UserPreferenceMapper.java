package com.resume.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.user.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好数据访问。
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}