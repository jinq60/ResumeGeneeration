package com.resume.user.service;

/**
 * 注册/登录验证码校验服务。
 * <p>
 * P0 提供占位实现（任意 6 位数字通过）；生产环境接入真实短信/邮件服务后，
 * 实现本接口并将 {@code app.auth.verify-code.mode} 改为相应模式注册 Bean。
 * 未配置任何实现时，注册接口将安全拒绝，防止占位验证码裸奔上线。
 * </p>
 */
public interface VerifyCodeService {

    /**
     * 校验验证码是否有效。
     *
     * @param code   用户输入的验证码
     * @param target 接收方标识（手机号或邮箱）
     */
    boolean verify(String code, String target);

    /**
     * 校验方式标识（用于日志与诊断）。
     */
    String mode();
}
