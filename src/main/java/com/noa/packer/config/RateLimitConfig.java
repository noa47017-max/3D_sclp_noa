package com.noa.packer.config;


import com.noa.packer.security.RateLimiter;
import com.noa.packer.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class RateLimitConfig {

    private final RateLimiter rateLimiter;

    public RateLimitConfig(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableAutoRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        rateLimiter.cleanup();
    }
}
