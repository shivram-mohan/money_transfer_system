package com.fidelity.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Small dedicated pool for outbound OTP emails so a slow SMTP server never
     * ties up Tomcat request threads.
     */
    @Bean(name = "otpMailExecutor")
    public Executor otpMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("otp-mail-");
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated pool for rewards notification emails (per-transaction alerts,
     * tier changes, monthly summaries) so they never contend with OTP delivery
     * or tie up request threads.
     */
    @Bean(name = "rewardMailExecutor")
    public Executor rewardMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("reward-mail-");
        executor.initialize();
        return executor;
    }
}
