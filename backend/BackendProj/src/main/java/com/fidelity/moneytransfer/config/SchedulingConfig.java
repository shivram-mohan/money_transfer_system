package com.fidelity.moneytransfer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task support for the rewards jobs
 * (inactivity downgrades and monthly summaries).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
