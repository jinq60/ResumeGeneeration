package com.resume.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 Spring 定时任务调度，供清理过期记录等周期性任务使用。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}