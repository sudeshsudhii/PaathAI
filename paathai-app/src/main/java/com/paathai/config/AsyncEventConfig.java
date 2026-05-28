package com.paathai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool configuration for async event fan-out.
 * Each consumer type gets its own pool to ensure failure isolation —
 * a slow Notes generation cannot starve Topic Detection threads.
 */
@Configuration
public class AsyncEventConfig {

    @Bean("topicDetectionExecutor")
    public Executor topicDetectionExecutor() {
        return buildExecutor("topic-detect-", 2, 4, 50);
    }

    @Bean("notesGenerationExecutor")
    public Executor notesGenerationExecutor() {
        return buildExecutor("notes-gen-", 2, 4, 50);
    }

    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor() {
        return buildExecutor("search-idx-", 2, 4, 50);
    }

    @Bean("generalEventExecutor")
    public Executor generalEventExecutor() {
        return buildExecutor("event-", 2, 8, 100);
    }

    private Executor buildExecutor(String prefix, int coreSize, int maxSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
