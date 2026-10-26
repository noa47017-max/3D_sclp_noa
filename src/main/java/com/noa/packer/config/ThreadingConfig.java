package com.noa.packer.config; 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
 public class ThreadingConfig {

    // 1. Thread Pool עבור בקשות המשתמשים הכלליות
    @Bean(name = "userRequestsExecutor")
    public ThreadPoolTaskExecutor userRequestsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 2 בקשות במקביל
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20); // תור המתנה לבקשות
        executor.setThreadNamePrefix("UserReq-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        return executor;
    }

    // 2. Thread Pool חזק עבור האלגוריתם והנמלים
    @Bean(name = "antAlgorithmExecutor")
    public ThreadPoolTaskExecutor antAlgorithmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();

        // משתמשים בכל הליבות עבור הנמלים, ואפילו קצת יותר
        executor.setCorePoolSize(cores * 2);
        executor.setMaxPoolSize(cores * 4);
        executor.setQueueCapacity(10_000);
        executor.setThreadNamePrefix("AntWorker-");
        executor.initialize();
        return executor;
    }

}