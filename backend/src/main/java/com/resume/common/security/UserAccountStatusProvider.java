package com.resume.common.security;

/**
 * 用户账号状态提供者（由 user 模块实现）。
 * <p>
 * 供 {@link JwtAuthenticationFilter} 在认证时校验账号是否仍可用，
 * 使管理员禁用/删除账号后旧 token 能及时失效。
 * </p>
 */
public interface UserAccountStatusProvider {

    /**
     * 账号是否可用（存在、未逻辑删除、未被禁用）。
     */
    boolean isEnabled(String userId);

    /**
     * 查询用户当前实际角色（如 ADMIN / USER），带短 TTL 缓存。
     * <p>
     * 供 {@link JwtAuthenticationFilter} 在 token claim 声明为 ADMIN 时回库核对，
     * 使管理员被降权后旧 access token 尽快失去管理员权限。
     * 用户不存在时返回 {@code null}；实现不可用时过滤器应保守降级为普通用户。
     * </p>
     */
    default String findRole(String userId) {
        return null;
    }
}
