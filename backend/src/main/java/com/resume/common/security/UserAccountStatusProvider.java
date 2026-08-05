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
}
