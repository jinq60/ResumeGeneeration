package com.resume.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 专用异步线程池。
 * <p>
 * 同时实现 {@link AsyncConfigurer} 提供 {@link AsyncUncaughtExceptionHandler}：
 * 任何 @Async("aiTaskExecutor") 方法抛出未捕获异常时，由统一处理器打印
 * 方法签名、参数列表与完整堆栈，便于生产环境排查 AI 任务静默失败的问题。
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncAiConfig implements AsyncConfigurer {

    private final AiProperties aiProperties;

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        AiProperties.ThreadPoolConfig pool = aiProperties.getThreadPool();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.getCoreSize());
        executor.setMaxPoolSize(pool.getMaxSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async AI task failed uncaught: method={}, params={}, error={}",
                    method.getName(),
                    Arrays.toString(params),
                    ex.getMessage(),
                    ex);
        };
    }
}
