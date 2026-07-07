package com.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService notificationExecutorService() {
        // Create a fixed thread pool of 10 threads for processing notifications
        return Executors.newFixedThreadPool(10);
    }
}
