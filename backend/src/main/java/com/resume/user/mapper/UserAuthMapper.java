package com.resume.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.user.entity.UserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 多方式认证绑定 Mapper。
 */
@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {
}
