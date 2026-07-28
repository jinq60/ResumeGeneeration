package com.resume.common.service;

/**
 * 限流器接口，支持多种后端实现。
 */
public interface RateLimiter {

    /**
     * 尝试获取请求许可。
     *
     * @param key          限流键（如 IP 地址）
     * @param maxRequests  时间窗口内最大请求数
     * @param windowMs     时间窗口（毫秒）
     * @return true 表示允许通过，false 表示被限流
     */
    boolean tryAcquire(String key, int maxRequests, long windowMs);
}
