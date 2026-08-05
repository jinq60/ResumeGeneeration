package com.resume.user.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.security.UserAccountStatusProvider;
import com.resume.user.entity.User;
import com.resume.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于数据库的用户状态校验实现，带短 TTL 内存缓存（15s），
 * 管理员禁用/删除账号后旧 token 会在数秒内失效。
 */
@Component
@RequiredArgsConstructor
public class UserStatusProvider implements UserAccountStatusProvider {

    private static final long TTL_MS = 15_000;
    private static final int MAX_CACHE_SIZE = 10_000;

    private final UserMapper userMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public boolean isEnabled(String userId) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(userId);
        if (entry != null && now - entry.timestamp < TTL_MS) {
            return entry.enabled;
        }
        User user = userMapper.selectById(userId);
        boolean enabled = user != null
                && !BizConstant.DELETED.equals(user.getDeleted())
                && !BizConstant.USER_STATUS_DISABLED.equals(user.getStatus());
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }
        cache.put(userId, new CacheEntry(enabled, now));
        return enabled;
    }

    private record CacheEntry(boolean enabled, long timestamp) {
    }
}
