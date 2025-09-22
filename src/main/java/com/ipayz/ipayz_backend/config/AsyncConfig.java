package com.ipayz.ipayz_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);     // keep a few threads always ready
        executor.setMaxPoolSize(10);     // don’t overload
        executor.setQueueCapacity(50);   // buffer for bursts
        executor.setThreadNamePrefix("EmailSender-");
        executor.initialize();
        return executor;
    }
}

