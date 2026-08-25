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
 * 基于数据库的用户状态/角色校验实现，带短 TTL 内存缓存（15s），
 * 管理员禁用/删除账号或降权后旧 token 会在数秒内失效。
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
        return load(userId).enabled();
    }

    @Override
    public String findRole(String userId) {
        return load(userId).role();
    }

    /**
     * 查询并缓存账号可用性与实际角色（同一 TTL 窗口内复用一次查询）。
     */
    private CacheEntry load(String userId) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(userId);
        if (entry != null && now - entry.timestamp() < TTL_MS) {
            return entry;
        }
        User user = userMapper.selectById(userId);
        boolean enabled = user != null
                && !BizConstant.DELETED.equals(user.getDeleted())
                && !BizConstant.USER_STATUS_DISABLED.equals(user.getStatus());
        // 角色为空的存量账号视为普通用户；用户不存在时角色为 null（由调用方保守降级）
        String role = user == null ? null
                : (user.getRole() == null ? BizConstant.USER_ROLE_USER : user.getRole());
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }
        entry = new CacheEntry(enabled, role, now);
        cache.put(userId, entry);
        return entry;
    }

    private record CacheEntry(boolean enabled, String role, long timestamp) {
    }
}
