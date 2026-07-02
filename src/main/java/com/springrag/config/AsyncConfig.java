package com.springrag.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} and provides a single, bounded executor for off-request AI work — answer
 * evaluation, report generation, and conversation post-processing (summary + memory extraction).
 *
 * <p>Why bounded: these tasks each make a multi-second LLM call. An unbounded pool would let a burst
 * of interview turns spawn unlimited concurrent inferences and overwhelm the model server. The queue
 * absorbs bursts; {@code CallerRunsPolicy} applies natural backpressure if the queue fills (the
 * caller runs the task itself rather than dropping it).
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String AI_EXECUTOR = "aiTaskExecutor";

    @Bean(name = AI_EXECUTOR)
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return aiTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // Async void methods can't propagate exceptions to a caller — log them instead of losing them.
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
