package com.resume.pdf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * PDF 导出专用线程池。
 * <p>
 * 队列满时直接拒绝（AbortPolicy），由调用方捕获后标记任务失败，
 * 避免 Playwright 任务在请求线程上同步执行导致 Tomcat 线程耗尽。
 * </p>
 */
@Configuration
public class PdfAsyncConfig {

    @Bean("pdfTaskExecutor")
    public Executor pdfTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("pdf-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
