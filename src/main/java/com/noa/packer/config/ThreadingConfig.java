package com.noa.packer.config; // התאימי את החבילה לשלך

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadingConfig {

        @Bean(name = "antAlgorithmExecutor")
        public ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

            // בדיקה דינמית של כמות הליבות שיש למחשב/שרת הנוכחי
            int cores = Runtime.getRuntime().availableProcessors();

            // 1. Core Pool Size - תואם לכמות הליבות הפיזית
            executor.setCorePoolSize(cores);

            // 2. Max Pool Size - פי 2 מכמות הליבות למקרי עומס
            executor.setMaxPoolSize(cores * 2);

            // 3. Queue Capacity - תור ענק כדי שאף משימה לא תידחה
            executor.setQueueCapacity(1000);

            executor.setThreadNamePrefix("AntWorker-");
            executor.initialize();
            return executor;
        }

}